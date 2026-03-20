package org.cardanofoundation.tokenmetadata.registry.it;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for CIP-26 (offchain metadata) flow.
 * Requires:
 * - PostgreSQL running
 * - API application started with TOKEN_METADATA_SYNC_JOB=true
 * - Test token registry git repo at GITHUB_TMP_FOLDER/GITHUB_PROJECT_NAME
 */
public class Cip26IntegrationIT extends BaseIntegrationIT {

    private static final String FULL_TOKEN_SUBJECT = "a0b1c2d3e4f5a0b1c2d3e4f5a0b1c2d3e4f5a0b1c2d3e4f5a0b1c2d354455354544f4b454e";
    private static final String MINIMAL_TOKEN_SUBJECT = "b1c2d3e4f5a0b1c2d3e4f5a0b1c2d3e4f5a0b1c2d3e4f5a0b1c2d3e44d494e544f4b454e";
    private static final String UNKNOWN_SUBJECT = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffff556e6b6e6f776e";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    static void setUp() {
        waitForApiReady();
        waitForSyncComplete();
    }

    private static void waitForSyncComplete() {
        log.info("Waiting for CIP-26 sync to complete (subject={}) ...", FULL_TOKEN_SUBJECT);
        await().atMost(Duration.ofMinutes(5))
                .pollInterval(Duration.ofSeconds(2))
                .ignoreExceptions()
                .until(() -> {
                    ResponseEntity<String> response = restTemplate.getForEntity(
                            API_BASE_URL + "/metadata/" + FULL_TOKEN_SUBJECT, String.class);
                    log.info("CIP-26 sync poll: status={}", response.getStatusCode());
                    return response.getStatusCode() == HttpStatus.OK;
                });
        log.info("CIP-26 sync complete.");
    }

    private static ResponseEntity<String> postJson(String url, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
    }

    @Nested
    @DisplayName("V1 - Single subject query")
    class V1SingleSubject {

        @Test
        void knownSubject_returnsAllProperties() throws Exception {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    API_BASE_URL + "/metadata/" + FULL_TOKEN_SUBJECT, String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            JsonNode json = objectMapper.readTree(response.getBody());
            assertThat(json.get("subject").asText()).isEqualTo(FULL_TOKEN_SUBJECT);
            assertThat(json.get("name").get("value").asText()).isEqualTo("Test Token Full");
            assertThat(json.get("ticker").get("value").asText()).isEqualTo("TSTF");
            assertThat(json.get("description").get("value").asText())
                    .isEqualTo("A test token with all properties for integration testing");
            assertThat(json.get("url").get("value").asText())
                    .isEqualTo("https://test.cardanofoundation.org");
            assertThat(json.get("decimals").get("value").asText()).isEqualTo("6");
        }

        @Test
        void unknownSubject_returnsNoContent() {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    API_BASE_URL + "/metadata/" + UNKNOWN_SUBJECT, String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }
    }

    @Nested
    @DisplayName("V1 - Property filter")
    class V1PropertyFilter {

        @Test
        void singleProperty_returnsOnlyThatProperty() throws Exception {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    API_BASE_URL + "/metadata/" + FULL_TOKEN_SUBJECT + "/properties/ticker", String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            JsonNode json = objectMapper.readTree(response.getBody());
            assertThat(json.get("subject").asText()).isEqualTo(FULL_TOKEN_SUBJECT);
            assertThat(json.get("ticker").get("value").asText()).isEqualTo("TSTF");
        }
    }

    @Nested
    @DisplayName("V1 - Batch query")
    class V1BatchQuery {

        @Test
        void mixedSubjects_returnsOnlyExisting() throws Exception {
            String body = String.format(
                    "{\"subjects\": [\"%s\", \"%s\"], \"properties\": []}",
                    FULL_TOKEN_SUBJECT, UNKNOWN_SUBJECT);

            ResponseEntity<String> response = postJson(API_BASE_URL + "/metadata/query", body);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            JsonNode subjects = objectMapper.readTree(response.getBody()).get("subjects");
            assertThat(subjects).hasSize(1);
            assertThat(subjects.get(0).get("subject").asText()).isEqualTo(FULL_TOKEN_SUBJECT);
            assertThat(subjects.get(0).get("name").get("value").asText()).isEqualTo("Test Token Full");
        }
    }

    @Nested
    @DisplayName("V2 - Single subject query")
    class V2SingleSubject {

        @Test
        void knownSubject_returnsMetadataWithSource() throws Exception {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    API_BASE_URL + "/api/v2/subjects/" + FULL_TOKEN_SUBJECT, String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            JsonNode json = objectMapper.readTree(response.getBody());
            JsonNode subject = json.get("subject");
            assertThat(subject.get("subject").asText()).isEqualTo(FULL_TOKEN_SUBJECT);

            JsonNode metadata = subject.get("metadata");
            assertThat(metadata.get("name").get("value").asText()).isEqualTo("Test Token Full");
            assertThat(metadata.get("name").get("source").asText()).isEqualTo("CIP_26");
        }

        @Test
        void unknownSubject_returns404() {
            assertThatThrownBy(() -> restTemplate.getForEntity(
                    API_BASE_URL + "/api/v2/subjects/" + UNKNOWN_SUBJECT, String.class))
                    .isInstanceOf(HttpClientErrorException.NotFound.class);
        }

        @Test
        void minimalToken_returnsOnlyNameAndDescription() throws Exception {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    API_BASE_URL + "/api/v2/subjects/" + MINIMAL_TOKEN_SUBJECT, String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            JsonNode metadata = objectMapper.readTree(response.getBody()).get("subject").get("metadata");
            assertThat(metadata.get("name").get("value").asText()).isEqualTo("Test Token Minimal");
            assertThat(metadata.get("description").get("value").asText())
                    .isEqualTo("A minimal test token with only required properties");
            JsonNode ticker = metadata.get("ticker");
            assertThat(ticker == null || ticker.isNull()).isTrue();
        }

        @Test
        void defaultQueryPriority_returnsCip68ThenCip26() throws Exception {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    API_BASE_URL + "/api/v2/subjects/" + FULL_TOKEN_SUBJECT, String.class);

            JsonNode queryPriority = objectMapper.readTree(response.getBody()).get("queryPriority");
            assertThat(queryPriority.get(0).asText()).isEqualTo("CIP_68");
            assertThat(queryPriority.get(1).asText()).isEqualTo("CIP_26");
        }
    }

    @Nested
    @DisplayName("V2 - Property filter")
    class V2PropertyFilter {

        @Test
        void requiredPlusOptionalProperties_returnsRequested() throws Exception {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    API_BASE_URL + "/api/v2/subjects/" + FULL_TOKEN_SUBJECT
                            + "?property=name&property=description&property=ticker",
                    String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            JsonNode metadata = objectMapper.readTree(response.getBody()).get("subject").get("metadata");
            assertThat(metadata.get("name").get("value").asText()).isEqualTo("Test Token Full");
            assertThat(metadata.get("description").get("value").asText())
                    .isEqualTo("A test token with all properties for integration testing");
            assertThat(metadata.get("ticker").get("value").asText()).isEqualTo("TSTF");
        }

        @Test
        void missingRequiredProperties_returnsBadRequest() {
            assertThatThrownBy(() -> restTemplate.getForEntity(
                    API_BASE_URL + "/api/v2/subjects/" + FULL_TOKEN_SUBJECT + "?property=ticker",
                    String.class))
                    .isInstanceOf(HttpClientErrorException.BadRequest.class);
        }
    }

    @Nested
    @DisplayName("V2 - CIPs details")
    class V2CipsDetails {

        @Test
        void showCipsDetails_returnsCip26StandardBlock() throws Exception {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    API_BASE_URL + "/api/v2/subjects/" + FULL_TOKEN_SUBJECT
                            + "?show_cips_details=true",
                    String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            JsonNode standards = objectMapper.readTree(response.getBody()).get("subject").get("standards");
            assertThat(standards).isNotNull();
            assertThat(standards.get("cip26")).isNotNull();
            assertThat(standards.get("cip26").get("name").get("value").asText())
                    .isEqualTo("Test Token Full");
            // CIP-26-only token should not have cip68 standard
            JsonNode cip68 = standards.get("cip68");
            assertThat(cip68 == null || cip68.isNull()).isTrue();
        }
    }

    @Nested
    @DisplayName("V2 - Batch query")
    class V2BatchQuery {

        @Test
        void mixedSubjects_returnsOnlyExisting() throws Exception {
            String body = String.format(
                    "{\"subjects\": [\"%s\", \"%s\"], \"properties\": []}",
                    FULL_TOKEN_SUBJECT, UNKNOWN_SUBJECT);

            ResponseEntity<String> response = postJson(API_BASE_URL + "/api/v2/subjects/query", body);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            JsonNode json = objectMapper.readTree(response.getBody());
            JsonNode subjects = json.get("subjects");
            assertThat(subjects).hasSize(1);
            assertThat(subjects.get(0).get("subject").asText()).isEqualTo(FULL_TOKEN_SUBJECT);
        }

        @Test
        void allUnknownSubjects_returnsEmptyList() throws Exception {
            String body = String.format(
                    "{\"subjects\": [\"%s\"], \"properties\": []}", UNKNOWN_SUBJECT);

            ResponseEntity<String> response = postJson(API_BASE_URL + "/api/v2/subjects/query", body);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            JsonNode subjects = objectMapper.readTree(response.getBody()).get("subjects");
            assertThat(subjects).isEmpty();
        }

        @Test
        void multipleKnownSubjects_returnsAll() throws Exception {
            String body = String.format(
                    "{\"subjects\": [\"%s\", \"%s\"], \"properties\": []}",
                    FULL_TOKEN_SUBJECT, MINIMAL_TOKEN_SUBJECT);

            ResponseEntity<String> response = postJson(API_BASE_URL + "/api/v2/subjects/query", body);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            JsonNode subjects = objectMapper.readTree(response.getBody()).get("subjects");
            assertThat(subjects).hasSize(2);
        }

        @Test
        void missingRequiredProperties_returnsBadRequest() {
            String body = String.format(
                    "{\"subjects\": [\"%s\"], \"properties\": [\"ticker\"]}", FULL_TOKEN_SUBJECT);

            assertThatThrownBy(() -> postJson(API_BASE_URL + "/api/v2/subjects/query", body))
                    .isInstanceOf(HttpClientErrorException.BadRequest.class);
        }

        @Test
        void withPropertyFilter_returnsOnlyRequestedProperties() throws Exception {
            String body = String.format(
                    "{\"subjects\": [\"%s\"], \"properties\": [\"name\", \"description\", \"ticker\"]}",
                    FULL_TOKEN_SUBJECT);

            ResponseEntity<String> response = postJson(API_BASE_URL + "/api/v2/subjects/query", body);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            JsonNode subjects = objectMapper.readTree(response.getBody()).get("subjects");
            assertThat(subjects).hasSize(1);

            JsonNode metadata = subjects.get(0).get("metadata");
            assertThat(metadata.get("name").get("value").asText()).isEqualTo("Test Token Full");
            assertThat(metadata.get("description").get("value").asText())
                    .isEqualTo("A test token with all properties for integration testing");
            assertThat(metadata.get("ticker").get("value").asText()).isEqualTo("TSTF");
        }
    }
}
