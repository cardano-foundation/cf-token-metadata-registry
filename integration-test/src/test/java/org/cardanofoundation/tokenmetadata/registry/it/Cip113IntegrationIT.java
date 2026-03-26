package org.cardanofoundation.tokenmetadata.registry.it;

import com.bloxbean.cardano.client.backend.blockfrost.service.BFBackendService;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for CIP-113 (programmable token) flow.
 * Requires:
 * - PostgreSQL running
 * - Yaci devnet running (yaci-store on port 8080, admin on port 10000)
 * - API application started with CIP113_REGISTRY_NFT_POLICY_IDS set to
 *   the always-true script's policy ID
 *
 * Flow: mint CIP-113 registry node NFT on devnet -> yaci-store indexes UTXO ->
 * Cip113EventListener parses datum -> cip113_registry_node table ->
 * V2 API query returns programmable_token_cip113 field
 */
public class Cip113IntegrationIT extends BaseIntegrationIT {

    // Test registry node fields
    private static final String REGISTERED_POLICY_ID = "aabbccdd11223344aabbccdd11223344aabbccdd11223344aabbccdd";
    private static final String NEXT_KEY = "ffffffffffffffffffffffffffffffffffffffffffffffffffffff";
    private static final String TRANSFER_LOGIC_SCRIPT = "1111111111111111111111111111111111111111111111111111111111";
    private static final String THIRD_PARTY_SCRIPT = "2222222222222222222222222222222222222222222222222222222222";
    private static final String GLOBAL_STATE_POLICY_ID = "3333333333333333333333333333333333333333333333333333333333";

    @BeforeAll
    static void setUp() throws Exception {
        waitForApiReady();

        // Skip entire test class if CIP-113 is not enabled on the API
        Assumptions.assumeTrue(isCip113Enabled(),
                "CIP-113 IT not enabled — set CIP113_IT_ENABLED=true and CIP113_REGISTRY_NFT_POLICY_IDS on the API");

        waitForYaciStoreReady();

        // Mint a CIP-113 registry node NFT on the devnet
        Cip113TestMinter minter = new Cip113TestMinter(new BFBackendService(YACI_STORE_URL, "Dummy"), restTemplate);

        // Verify the API is configured with the correct registry NFT policy ID
        String expectedPolicyId = Cip113TestMinter.getRegistryNftPolicyId();
        log.info("CIP-113 registry NFT policy ID (always-true script): {}", expectedPolicyId);
        log.info("Ensure the API is started with CIP113_REGISTRY_NFT_POLICY_IDS={}", expectedPolicyId);

        minter.mintRegistryNode(
                REGISTERED_POLICY_ID,
                NEXT_KEY,
                TRANSFER_LOGIC_SCRIPT,
                THIRD_PARTY_SCRIPT,
                GLOBAL_STATE_POLICY_ID
        );

        // Wait for the API to index the registry node
        waitForCip113Indexed(REGISTERED_POLICY_ID);
    }

    private static void waitForCip113Indexed(String registeredPolicyId) {
        log.info("Waiting for CIP-113 registry node to be indexed (registeredPolicyId={}) ...", registeredPolicyId);
        await().atMost(Duration.ofMinutes(5))
                .pollInterval(Duration.ofSeconds(2))
                .ignoreExceptions()
                .until(() -> {
                    // Query the V2 API - even if the subject doesn't have CIP-26/CIP-68 metadata,
                    // we can check via a different mechanism. Instead, we'll also mint a CIP-68 token
                    // with the same policy, or we query the prometheus metrics for cip113 count.
                    // Simplest approach: query prometheus for the cip113 count
                    ResponseEntity<String> response = restTemplate.getForEntity(
                            API_BASE_URL + "/actuator/prometheus", String.class);
                    if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                        // Check if cip113 count > 0
                        boolean indexed = response.getBody().contains("cftr_tokens_cip113_count")
                                && !response.getBody().contains("cftr_tokens_cip113_count 0.0");
                        log.info("CIP-113 index poll via metrics: indexed={}", indexed);
                        return indexed;
                    }
                    return false;
                });
        log.info("CIP-113 registry node indexed successfully.");
    }

    @Nested
    @DisplayName("V2 - Programmable token enrichment")
    class V2ProgrammableToken {

        @Test
        void cip113MetricCountsRegisteredToken() {
            // The metrics gauge refreshes every 30s, so poll until it reflects the minted token
            await().atMost(Duration.ofSeconds(45))
                    .pollInterval(Duration.ofSeconds(5))
                    .ignoreExceptions()
                    .untilAsserted(() -> {
                        ResponseEntity<String> response = restTemplate.getForEntity(
                                API_BASE_URL + "/actuator/prometheus", String.class);

                        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                        assertThat(response.getBody()).isNotNull();

                        String body = response.getBody();
                        boolean hasRegisteredToken = body.lines()
                                .filter(line -> line.startsWith("cftr_tokens_cip113_count"))
                                .anyMatch(line -> {
                                    try {
                                        String valueStr = line.substring(line.lastIndexOf(' ') + 1).trim();
                                        double value = Double.parseDouble(valueStr);
                                        return value >= 1.0;
                                    } catch (NumberFormatException _) {
                                        return false;
                                    }
                                });
                        assertThat(hasRegisteredToken)
                                .as("cftr_tokens_cip113_count should be >= 1")
                                .isTrue();
                    });
        }
    }

    @Nested
    @DisplayName("V2 - CIP-113 with CIP-68 token")
    class V2Cip113WithCip68 {

        /**
         * This test mints both a CIP-68 FT and a CIP-113 registry node using the SAME
         * policy ID, then verifies that the V2 API returns both CIP-68 metadata AND the
         * CIP-113 extension in the extensions map.
         */
        @Test
        void cip68TokenWithCip113RegistryShowsBothInResponse() throws Exception {
            // Mint a CIP-68 FT using the same always-true script
            Cip68TestMinter cip68Minter = new Cip68TestMinter(
                    new BFBackendService(YACI_STORE_URL, "Dummy"), restTemplate);

            Cip68TestMinter.MintResult cip68Result = cip68Minter.mintCip68FungibleToken(
                    "ProgToken", "A programmable test token", "PTOK", 6, 500_000);

            String cip68Subject = cip68Result.policyId() + cip68Result.assetNameHex();

            // Now mint a CIP-113 registry node for the same policy ID
            Cip113TestMinter cip113Minter = new Cip113TestMinter(
                    new BFBackendService(YACI_STORE_URL, "Dummy"), restTemplate);

            cip113Minter.mintRegistryNode(
                    cip68Result.policyId(),
                    NEXT_KEY,
                    TRANSFER_LOGIC_SCRIPT,
                    THIRD_PARTY_SCRIPT,
                    ""
            );

            // Wait for CIP-68 to be indexed
            log.info("Waiting for CIP-68 + CIP-113 combined token to be indexed...");
            await().atMost(Duration.ofMinutes(5))
                    .pollInterval(Duration.ofSeconds(2))
                    .ignoreExceptions()
                    .until(() -> {
                        ResponseEntity<String> resp = restTemplate.getForEntity(
                                API_BASE_URL + "/api/v2/subjects/" + cip68Subject, String.class);
                        if (resp.getStatusCode() == HttpStatus.OK) {
                            DocumentContext json = JsonPath.parse(resp.getBody());
                            boolean hasCip68 = "CIP_68".equals(json.read("$.subject.metadata.name.source", String.class));
                            boolean hasCip113 = json.read("$.subject.extensions.cip113") != null;
                            log.info("Combined poll: hasCip68={}, hasCip113={}", hasCip68, hasCip113);
                            return hasCip68 && hasCip113;
                        }
                        return false;
                    });

            // Final assertions
            ResponseEntity<String> response = restTemplate.getForEntity(
                    API_BASE_URL + "/api/v2/subjects/" + cip68Subject, String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            DocumentContext json = JsonPath.parse(response.getBody());

            // Verify CIP-68 metadata
            assertThat(json.read("$.subject.metadata.name.value", String.class)).isEqualTo("ProgToken");
            assertThat(json.read("$.subject.metadata.name.source", String.class)).isEqualTo("CIP_68");
            assertThat(json.read("$.subject.metadata.description.value", String.class)).isEqualTo("A programmable test token");

            // Verify CIP-113 extension
            assertThat(json.read("$.subject.extensions.cip113.transfer_logic_script", String.class)).isEqualTo(TRANSFER_LOGIC_SCRIPT);
            assertThat(json.read("$.subject.extensions.cip113.third_party_transfer_logic_script", String.class)).isEqualTo(THIRD_PARTY_SCRIPT);
        }
    }

    /**
     * Check if CIP-113 integration tests should run.
     * Requires CIP113_IT_ENABLED=true environment variable — this must be set alongside
     * the API's CIP113_REGISTRY_NFT_POLICY_IDS in the CI environment.
     */
    private static boolean isCip113Enabled() {
        boolean enabled = "true".equalsIgnoreCase(System.getenv("CIP113_IT_ENABLED"));
        log.info("CIP-113 integration test enabled check: {} (CIP113_IT_ENABLED={})",
                enabled, System.getenv("CIP113_IT_ENABLED"));
        return enabled;
    }

}
