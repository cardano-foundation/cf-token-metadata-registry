package org.cardanofoundation.tokenmetadata.registry.it;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.backend.api.BackendService;
import com.bloxbean.cardano.client.common.cbor.CborSerializationUtil;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.function.helper.SignerProviders;
import com.bloxbean.cardano.client.plutus.spec.*;
import com.bloxbean.cardano.client.quicktx.QuickTxBuilder;
import com.bloxbean.cardano.client.quicktx.ScriptTx;
import com.bloxbean.cardano.client.transaction.spec.Asset;
import com.bloxbean.cardano.client.util.HexUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.time.Duration;
import java.util.List;

import static org.awaitility.Awaitility.await;

/**
 * Utility to mint a CIP-113 registry node NFT on a yaci devnet.
 * <p>
 * A CIP-113 registry node is an NFT with quantity=1 whose inline datum is a
 * ConstrPlutusData(0, [key, next, Constr(0,[transferLogic]), Constr(0,[thirdParty]), globalState]).
 * The NFT's token name equals the registered policy ID (the "key" field).
 * <p>
 * For integration testing, we use an always-true PlutusV2 script as the minting policy.
 * The policy ID of this script becomes the "registry NFT policy ID" that the API must
 * be configured to monitor (via CIP113_REGISTRY_NFT_POLICY_ID).
 */
@Slf4j
@RequiredArgsConstructor
public class Cip113TestMinter {

    private static final String DEVKIT_ADMIN_BASE_URL = "http://localhost:10000/";

    // Always-true PlutusV2 script — same as used in Cip68TestMinter
    private static final PlutusV2Script ALWAYS_TRUE_SCRIPT = PlutusV2Script.builder()
            .type("PlutusScriptV2")
            .cborHex("49480100002221200101")
            .build();

    private final BackendService backendService;
    private final RestTemplate restTemplate;
    private final Account senderAccount = new Account(Networks.testnet());

    public record MintResult(String registryNftPolicyId, String registeredPolicyId, String txHash) {}

    /**
     * Returns the policy ID of the always-true script used for registry NFTs.
     * This is the value that must be set as CIP113_REGISTRY_NFT_POLICY_ID.
     */
    public static String getRegistryNftPolicyId() {
        try {
            return ALWAYS_TRUE_SCRIPT.getPolicyId();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get always-true script policy ID", e);
        }
    }

    /**
     * Mints a CIP-113 registry node NFT on the devnet.
     *
     * @param registeredPolicyId          the policy ID of the "registered" programmable token (the datum's {@code key})
     * @param next                        the {@code next} pointer in the sorted linked list
     * @param transferLogicScript         script hash for transfer validation
     * @param thirdPartyTransferLogicScript script hash for third-party transfers
     * @param globalStatePolicyId         optional global state policy ID
     * @return the mint result with registry NFT policy ID and registered policy ID
     */
    public MintResult mintRegistryNode(String registeredPolicyId,
                                        String next,
                                        String transferLogicScript,
                                        String thirdPartyTransferLogicScript,
                                        String globalStatePolicyId) throws Exception {

        String senderAddress = senderAccount.baseAddress();

        // Fund the sender and wait for UTXOs
        topUpFund(senderAddress, 50000);
        await().atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(2))
                .ignoreExceptions()
                .until(() -> {
                    Result<List<Utxo>> utxos = backendService.getUtxoService().getUtxos(senderAddress, 1, 1);
                    boolean funded = utxos.isSuccessful() && !utxos.getValue().isEmpty();
                    log.info("Waiting for topup UTXOs: funded={}", funded);
                    return funded;
                });

        // Build the registry node datum
        PlutusData datum = buildRegistryNodeDatum(
                registeredPolicyId, next,
                transferLogicScript, thirdPartyTransferLogicScript,
                globalStatePolicyId);

        // The NFT token name is the registered policy ID (hex bytes, 0x prefix for cardano-client-lib)
        Asset registryNft = new Asset("0x" + registeredPolicyId, BigInteger.ONE);

        // Mint the registry node NFT to the sender's address with the inline datum
        ScriptTx scriptTx = new ScriptTx()
                .mintAsset(ALWAYS_TRUE_SCRIPT, List.of(registryNft), PlutusData.unit(),
                        senderAddress, datum);

        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);
        Result<String> result = quickTxBuilder.compose(scriptTx)
                .feePayer(senderAddress)
                .withSigner(SignerProviders.signerFrom(senderAccount))
                .completeAndWait(txHash -> log.info("CIP-113 registry node mint tx submitted: {}", txHash));

        if (!result.isSuccessful()) {
            throw new RuntimeException("Failed to mint CIP-113 registry node: " + result.getResponse());
        }

        String registryNftPolicyId = ALWAYS_TRUE_SCRIPT.getPolicyId();
        log.info("CIP-113 registry node minted. TxHash: {}, RegistryNftPolicyId: {}, RegisteredPolicyId: {}",
                result.getValue(), registryNftPolicyId, registeredPolicyId);

        return new MintResult(registryNftPolicyId, registeredPolicyId, result.getValue());
    }

    /**
     * Builds a CIP-113 RegistryNode datum:
     * ConstrPlutusData(0, [key, next, Constr(0,[transferLogic]), Constr(0,[thirdParty]), globalState])
     */
    private static PlutusData buildRegistryNodeDatum(String key, String next,
                                                      String transferLogic, String thirdPartyLogic,
                                                      String globalState) {
        return ConstrPlutusData.of(0,
                BytesPlutusData.of(HexUtil.decodeHexString(key)),
                BytesPlutusData.of(HexUtil.decodeHexString(next)),
                ConstrPlutusData.of(0, BytesPlutusData.of(HexUtil.decodeHexString(transferLogic))),
                ConstrPlutusData.of(0, BytesPlutusData.of(HexUtil.decodeHexString(thirdPartyLogic))),
                BytesPlutusData.of(globalState.isEmpty() ? new byte[0] : HexUtil.decodeHexString(globalState))
        );
    }

    private void topUpFund(String address, long adaAmount) {
        try {
            String url = DEVKIT_ADMIN_BASE_URL + "local-cluster/api/addresses/topup";
            String json = String.format("{\"address\": \"%s\", \"adaAmount\": %d}", address, adaAmount);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<String> response = restTemplate.postForEntity(url, new HttpEntity<>(json, headers), String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Funds topped up successfully for {}", address);
            } else {
                log.warn("Failed to top up funds. Response code: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.warn("Could not topup address: {}", e.getMessage());
        }
    }

}
