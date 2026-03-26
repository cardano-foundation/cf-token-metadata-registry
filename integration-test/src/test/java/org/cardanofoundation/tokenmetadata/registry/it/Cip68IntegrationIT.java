package org.cardanofoundation.tokenmetadata.registry.it;

import com.bloxbean.cardano.client.backend.blockfrost.service.BFBackendService;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
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
                        DocumentContext json = JsonPath.parse(response.getBody());
                        String nameSource = json.read("$.subject.metadata.name.source", String.class);
                        boolean indexed = "CIP_68".equals(nameSource);
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

            DocumentContext json = JsonPath.parse(response.getBody());
            assertThat(json.read("$.subject.subject", String.class)).isEqualTo(subject());

            assertThat(json.read("$.subject.metadata.name.value", String.class)).isEqualTo(TOKEN_NAME);
            assertThat(json.read("$.subject.metadata.name.source", String.class)).isEqualTo("CIP_68");
            assertThat(json.read("$.subject.metadata.description.value", String.class)).isEqualTo(TOKEN_DESCRIPTION);
            assertThat(json.read("$.subject.metadata.description.source", String.class)).isEqualTo("CIP_68");
            assertThat(json.read("$.subject.metadata.ticker.value", String.class)).isEqualTo(TOKEN_TICKER);
            assertThat(json.read("$.subject.metadata.decimals.value", Integer.class)).isEqualTo(TOKEN_DECIMALS);
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

            DocumentContext json = JsonPath.parse(response.getBody());
            assertThat(json.read("$.subject.standards.cip68", Object.class)).isNotNull();
            assertThat(json.read("$.subject.standards.cip68.name", String.class)).isEqualTo(TOKEN_NAME);
            assertThat(json.read("$.subject.standards.cip68.description", String.class)).isEqualTo(TOKEN_DESCRIPTION);
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

            DocumentContext json = JsonPath.parse(response.getBody());
            assertThat(json.read("$.subject.metadata.name.value", String.class)).isEqualTo(TOKEN_NAME);
            assertThat(json.read("$.subject.metadata.name.source", String.class)).isEqualTo("CIP_68");

            assertThat(json.read("$.queryPriority[0]", String.class)).isEqualTo("CIP_26");
            assertThat(json.read("$.queryPriority[1]", String.class)).isEqualTo("CIP_68");
        }
    }
}
