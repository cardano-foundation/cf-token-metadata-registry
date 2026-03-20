package org.cardanofoundation.tokenmetadata.registry.it;

import com.bloxbean.cardano.client.backend.blockfrost.service.BFBackendService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Integration tests for CIP-68 (on-chain metadata) flow.
 * Requires:
 * - PostgreSQL running
 * - Yaci devnet running (yaci-store on port 8080, admin on port 10000)
 * - API application started and connected to the devnet node
 *
 * Flow: mint CIP-68 FT on devnet -> yaci-store indexes UTXO -> event listener
 * parses datum -> metadata_reference_nft table -> API query returns metadata
 */
public class Cip68IntegrationIT extends BaseIntegrationIT {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String TOKEN_NAME = "IntTestFT";
    private static final String TOKEN_DESCRIPTION = "Integration test fungible token";
    private static final String TOKEN_TICKER = "ITFT";
    private static final int TOKEN_DECIMALS = 6;

    private static Cip68TestMinter.MintResult mintResult;

    @BeforeAll
    static void setUp() throws Exception {
        waitForApiReady();
        waitForYaciStoreReady();

        // Mint a CIP-68 FT on the devnet
        Cip68TestMinter minter = new Cip68TestMinter(new BFBackendService(YACI_STORE_URL, "Dummy"), restTemplate);
        mintResult = minter.mintCip68FungibleToken(
                TOKEN_NAME, TOKEN_DESCRIPTION, TOKEN_TICKER, TOKEN_DECIMALS, 1_000_000);

        // Wait for the API to index the minted token
        String subject = mintResult.policyId() + mintResult.assetNameHex();
        waitForCip68Indexed(subject);
    }

    private static void waitForCip68Indexed(String subject) {
        log.info("Waiting for CIP-68 token to be indexed (subject={}) ...", subject);
        await().atMost(Duration.ofMinutes(5))
                .pollInterval(Duration.ofSeconds(2))
                .ignoreExceptions()
                .until(() -> {
                    ResponseEntity<String> response = restTemplate.getForEntity(
                            API_BASE_URL + "/api/v2/subjects/" + subject, String.class);
                    if (response.getStatusCode() == HttpStatus.OK) {
                        JsonNode json = objectMapper.readTree(response.getBody());
                        JsonNode metadata = json.get("subject").get("metadata");
                        boolean indexed = metadata.get("name") != null
                                && "CIP_68".equals(metadata.get("name").get("source").asText());
                        log.info("CIP-68 index poll: status={}, indexed={}", response.getStatusCode(), indexed);
                        return indexed;
                    }
                    log.info("CIP-68 index poll: status={}, not ready yet", response.getStatusCode());
                    return false;
                });
        log.info("CIP-68 token indexed successfully.");
    }

    private static String subject() {
        return mintResult.policyId() + mintResult.assetNameHex();
    }

    @Nested
    @DisplayName("V2 - On-chain metadata query")
    class V2OnChainMetadata {

        @Test
        void returnsAllMetadataFields() throws Exception {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    API_BASE_URL + "/api/v2/subjects/" + subject(), String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            JsonNode json = objectMapper.readTree(response.getBody());
            JsonNode subjectNode = json.get("subject");
            assertThat(subjectNode.get("subject").asText()).isEqualTo(subject());

            JsonNode metadata = subjectNode.get("metadata");
            assertThat(metadata.get("name").get("value").asText()).isEqualTo(TOKEN_NAME);
            assertThat(metadata.get("name").get("source").asText()).isEqualTo("CIP_68");
            assertThat(metadata.get("description").get("value").asText()).isEqualTo(TOKEN_DESCRIPTION);
            assertThat(metadata.get("description").get("source").asText()).isEqualTo("CIP_68");
            assertThat(metadata.get("ticker").get("value").asText()).isEqualTo(TOKEN_TICKER);
            assertThat(metadata.get("decimals").get("value").asInt()).isEqualTo(TOKEN_DECIMALS);
        }
    }

    @Nested
    @DisplayName("V2 - CIPs details")
    class V2CipsDetails {

        @Test
        void showCipsDetails_returnsCip68StandardBlock() throws Exception {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    API_BASE_URL + "/api/v2/subjects/" + subject() + "?show_cips_details=true",
                    String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            JsonNode standards = objectMapper.readTree(response.getBody()).get("subject").get("standards");
            assertThat(standards).isNotNull();

            JsonNode cip68 = standards.get("cip68");
            assertThat(cip68).isNotNull();
            assertThat(cip68.get("name").asText()).isEqualTo(TOKEN_NAME);
            assertThat(cip68.get("description").asText()).isEqualTo(TOKEN_DESCRIPTION);
        }
    }

    @Nested
    @DisplayName("V2 - Query priority")
    class V2QueryPriority {

        @Test
        void cip26Priority_fallsBackToCip68WhenNoCip26Data() throws Exception {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    API_BASE_URL + "/api/v2/subjects/" + subject()
                            + "?query_priority=CIP_26&query_priority=CIP_68",
                    String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            JsonNode json = objectMapper.readTree(response.getBody());
            JsonNode metadata = json.get("subject").get("metadata");
            assertThat(metadata.get("name").get("value").asText()).isEqualTo(TOKEN_NAME);
            assertThat(metadata.get("name").get("source").asText()).isEqualTo("CIP_68");

            JsonNode queryPriority = json.get("queryPriority");
            assertThat(queryPriority.get(0).asText()).isEqualTo("CIP_26");
            assertThat(queryPriority.get(1).asText()).isEqualTo("CIP_68");
        }
    }
}
