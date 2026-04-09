package org.cardanofoundation.tokenmetadata.registry.it;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.backend.api.BackendService;
import com.bloxbean.cardano.client.cip.cip68.CIP68FT;
import com.bloxbean.cardano.client.cip.cip68.CIP68ReferenceToken;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.function.helper.SignerProviders;
import com.bloxbean.cardano.client.plutus.spec.PlutusData;
import com.bloxbean.cardano.client.plutus.spec.PlutusV2Script;
import com.bloxbean.cardano.client.quicktx.QuickTxBuilder;
import com.bloxbean.cardano.client.quicktx.ScriptTx;
import com.bloxbean.cardano.client.transaction.spec.Asset;
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
 * Utility to mint CIP-68 token pairs (reference NFT + user FT) on a yaci devnet.
 * Uses an always-true PlutusV2 script as the minting policy — simplest approach
 * that works with the CIP-68 minting API (ScriptTx.mintAsset with inline datum).
 */
@Slf4j
@RequiredArgsConstructor
public class Cip68TestMinter {

    private static final String DEVKIT_ADMIN_BASE_URL = "http://localhost:10000/";

    // Always-true PlutusV2 script — effectively no validation, just like a NativeScript
    private static final PlutusV2Script ALWAYS_TRUE_SCRIPT = PlutusV2Script.builder()
            .type("PlutusScriptV2")
            .cborHex("49480100002221200101")
            .build();

    private final BackendService backendService;
    private final RestTemplate restTemplate;
    private final Account senderAccount = new Account(Networks.testnet());

    public record MintResult(String policyId, String assetNameHex, String txHash) {}

    /**
     * Mints a CIP-68 FT token pair on the devnet:
     * - Reference NFT (label 100 / prefix 000643b0) with inline datum containing metadata
     * - User FT (label 333 / prefix 0014df10) with specified quantity
     */
    public MintResult mintCip68FungibleToken(String tokenName, String description,
                                              String ticker, int decimals,
                                              long userTokenQty) throws Exception {
        String senderAddress = senderAccount.baseAddress();

        // Fund the sender and wait for UTXOs to appear
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

        // Build CIP-68 FT metadata using the high-level API
        CIP68FT ft = CIP68FT.create()
                .name(tokenName)
                .description(description)
                .ticker(ticker)
                .decimals(decimals);

        CIP68ReferenceToken referenceToken = ft.getReferenceToken();
        Asset userToken = ft.getAsset(BigInteger.valueOf(userTokenQty));
        Asset refToken = referenceToken.getAsset(BigInteger.ONE);
        PlutusData datum = referenceToken.getDatumAsPlutusData();

        // Reference token goes to a script address (standard CIP-68 pattern)
        String referenceTokenReceiver = AddressProvider.getEntAddress(ALWAYS_TRUE_SCRIPT, Networks.testnet()).toBech32();

        // User token goes to sender
        String userTokenReceiver = senderAddress;

        // Build minting transaction
        ScriptTx scriptTx = new ScriptTx()
                .mintAsset(ALWAYS_TRUE_SCRIPT, List.of(refToken), PlutusData.unit(), referenceTokenReceiver, datum)
                .mintAsset(ALWAYS_TRUE_SCRIPT, List.of(userToken), PlutusData.unit(), userTokenReceiver);

        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);
        Result<String> result = quickTxBuilder.compose(scriptTx)
                .feePayer(senderAddress)
                .withSigner(SignerProviders.signerFrom(senderAccount))
                .completeAndWait(txHash -> log.info("CIP-68 FT mint tx submitted: {}", txHash));

        if (!result.isSuccessful()) {
            throw new RuntimeException("Failed to mint CIP-68 token: " + result.getResponse());
        }

        String policyId = ALWAYS_TRUE_SCRIPT.getPolicyId();
        // getAssetNameAsHex() returns "0x0014df10..." — strip the "0x" prefix for our subject format
        String ftAssetNameHex = ft.getAssetNameAsHex().replaceFirst("^0x", "");

        log.info("CIP-68 FT minted. TxHash: {}, PolicyId: {}, FT AssetName: {}",
                result.getValue(), policyId, ftAssetNameHex);

        return new MintResult(policyId, ftAssetNameHex, result.getValue());
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
