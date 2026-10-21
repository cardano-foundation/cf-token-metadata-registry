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
import com.bloxbean.cardano.client.plutus.spec.BigIntPlutusData;
import com.bloxbean.cardano.client.plutus.spec.BytesPlutusData;
import com.bloxbean.cardano.client.plutus.spec.ConstrPlutusData;
import com.bloxbean.cardano.client.plutus.spec.MapPlutusData;
import com.bloxbean.cardano.client.plutus.spec.PlutusData;
import com.bloxbean.cardano.client.plutus.spec.PlutusV2Script;
import com.bloxbean.cardano.client.quicktx.QuickTxBuilder;
import com.bloxbean.cardano.client.quicktx.ScriptTx;
import com.bloxbean.cardano.client.transaction.spec.Asset;
import com.bloxbean.cardano.client.util.HexUtil;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

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

    /** CIP-68 user-token label prefixes. */
    public static final String NFT_PREFIX = "000de140";
    public static final String FT_PREFIX = "0014df10";
    public static final String RFT_PREFIX = "001bc280";
    private static final String REFERENCE_PREFIX = "000643b0";

    /**
     * A CIP-68 token minted with a hand-built datum, so that it can break CIP-68 on purpose (a missing field, a version
     * CIP-68 does not define, a logo that is not a URI) and pair one reference NFT with user tokens of several labels.
     *
     * @param baseName          the asset name without its label prefix (text, unique per test run)
     * @param userTokenPrefixes the label prefixes of the user tokens minted with the reference NFT, in the same transaction
     * @param textProperties    the datum's text properties (name, description, ticker, logo, image, ...), as written
     * @param decimals          the datum's decimals, or null for none
     * @param version           the datum's version, as written (CIP-68 defines 1 to 4)
     */
    public record Cip68Token(String baseName, List<String> userTokenPrefixes, Map<String, String> textProperties,
                             @Nullable Long decimals, long version) {

        public String baseNameHex() {
            return HexUtil.encodeHexString(baseName.getBytes(StandardCharsets.UTF_8));
        }

        /** The subject of the token's user token with the given label prefix. */
        public String subject(String prefix) {
            return policyId() + prefix + baseNameHex();
        }
    }

    /**
     * Mints a CIP-68 reference NFT with a hand-built inline datum {@code Constr 0 [metadata, version]} and its user
     * tokens, in one transaction: the reference NFT goes to a script address, the user tokens to the sender.
     */
    public String mintCip68Token(Cip68Token token, long userTokenQty) throws Exception {
        String senderAddress = fundSender();

        MapPlutusData metadata = new MapPlutusData();
        token.textProperties().forEach((key, value) -> metadata.put(BytesPlutusData.of(key), BytesPlutusData.of(value)));
        if (token.decimals() != null) {
            metadata.put(BytesPlutusData.of("decimals"), BigIntPlutusData.of(token.decimals()));
        }
        PlutusData datum = ConstrPlutusData.of(0, metadata, BigIntPlutusData.of(token.version()));

        Asset referenceNft = new Asset("0x" + REFERENCE_PREFIX + token.baseNameHex(), BigInteger.ONE);
        List<Asset> userTokens = token.userTokenPrefixes().stream()
                .map(prefix -> new Asset("0x" + prefix + token.baseNameHex(),
                        NFT_PREFIX.equals(prefix) ? BigInteger.ONE : BigInteger.valueOf(userTokenQty)))
                .toList();

        String referenceTokenReceiver = AddressProvider.getEntAddress(ALWAYS_TRUE_SCRIPT, Networks.testnet()).toBech32();
        ScriptTx scriptTx = new ScriptTx()
                .mintAsset(ALWAYS_TRUE_SCRIPT, List.of(referenceNft), PlutusData.unit(), referenceTokenReceiver, datum)
                .mintAsset(ALWAYS_TRUE_SCRIPT, userTokens, PlutusData.unit(), senderAddress);
        String txHash = submit(scriptTx, senderAddress);
        log.info("CIP-68 token {} minted with user tokens {}: {}", token.baseName(), token.userTokenPrefixes(), txHash);
        return txHash;
    }

    /** The policy id of every token this minter mints (an always-true script). */
    public static String policyId() {
        try {
            return ALWAYS_TRUE_SCRIPT.getPolicyId();
        } catch (Exception e) {
            throw new IllegalStateException("the policy id of the always-true script cannot be computed", e);
        }
    }

    private String fundSender() {
        String senderAddress = senderAccount.baseAddress();
        topUpFund(senderAddress, 50000);
        await().atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(2))
                .ignoreExceptions()
                .until(() -> {
                    Result<List<Utxo>> utxos = backendService.getUtxoService().getUtxos(senderAddress, 1, 1);
                    return utxos.isSuccessful() && !utxos.getValue().isEmpty();
                });
        return senderAddress;
    }

    private String submit(ScriptTx scriptTx, String senderAddress) {
        Result<String> result = new QuickTxBuilder(backendService).compose(scriptTx)
                .feePayer(senderAddress)
                .withSigner(SignerProviders.signerFrom(senderAccount))
                .completeAndWait(txHash -> log.info("CIP-68 mint tx submitted: {}", txHash));
        if (!result.isSuccessful()) {
            throw new IllegalStateException("Failed to mint CIP-68 token: " + result.getResponse());
        }
        return result.getValue();
    }

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
