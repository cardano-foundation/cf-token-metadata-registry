package org.cardanofoundation.tokenmetadata.registry.it;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
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
import java.util.List;

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
        void knownSubject_returnsAllProperties() {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    API_BASE_URL + "/metadata/" + FULL_TOKEN_SUBJECT, String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            DocumentContext json = JsonPath.parse(response.getBody());
            assertThat(json.read("$.subject", String.class)).isEqualTo(FULL_TOKEN_SUBJECT);
            assertThat(json.read("$.name.value", String.class)).isEqualTo("Test Token Full");
            assertThat(json.read("$.ticker.value", String.class)).isEqualTo("TSTF");
            assertThat(json.read("$.description.value", String.class))
                    .isEqualTo("A test token with all properties for integration testing");
            assertThat(json.read("$.url.value", String.class))
                    .isEqualTo("https://test.cardanofoundation.org");
            assertThat(json.read("$.decimals.value", String.class)).isEqualTo("6");
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
        void singleProperty_returnsOnlyThatProperty() {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    API_BASE_URL + "/metadata/" + FULL_TOKEN_SUBJECT + "/properties/ticker", String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            DocumentContext json = JsonPath.parse(response.getBody());
            assertThat(json.read("$.subject", String.class)).isEqualTo(FULL_TOKEN_SUBJECT);
            assertThat(json.read("$.ticker.value", String.class)).isEqualTo("TSTF");
        }
    }

    @Nested
    @DisplayName("V1 - Batch query")
    class V1BatchQuery {

        @Test
        void mixedSubjects_returnsOnlyExisting() {
            String body = String.format(
                    "{\"subjects\": [\"%s\", \"%s\"], \"properties\": []}",
                    FULL_TOKEN_SUBJECT, UNKNOWN_SUBJECT);

            ResponseEntity<String> response = postJson(API_BASE_URL + "/metadata/query", body);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            DocumentContext json = JsonPath.parse(response.getBody());
            assertThat(json.read("$.subjects", List.class)).hasSize(1);
            assertThat(json.read("$.subjects[0].subject", String.class)).isEqualTo(FULL_TOKEN_SUBJECT);
            assertThat(json.read("$.subjects[0].name.value", String.class)).isEqualTo("Test Token Full");
        }
    }

    @Nested
    @DisplayName("V2 - Single subject query")
    class V2SingleSubject {

        @Test
        void knownSubject_returnsMetadataWithSource() {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    API_BASE_URL + "/api/v2/subjects/" + FULL_TOKEN_SUBJECT, String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            DocumentContext json = JsonPath.parse(response.getBody());
            assertThat(json.read("$.subject.subject", String.class)).isEqualTo(FULL_TOKEN_SUBJECT);
            assertThat(json.read("$.subject.metadata.name.value", String.class)).isEqualTo("Test Token Full");
            assertThat(json.read("$.subject.metadata.name.source", String.class)).isEqualTo("CIP_26");
        }

        @Test
        void unknownSubject_returns404() {
            assertThatThrownBy(() -> restTemplate.getForEntity(
                    API_BASE_URL + "/api/v2/subjects/" + UNKNOWN_SUBJECT, String.class))
                    .isInstanceOf(HttpClientErrorException.NotFound.class);
        }

        @Test
        void minimalToken_returnsOnlyNameAndDescription() {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    API_BASE_URL + "/api/v2/subjects/" + MINIMAL_TOKEN_SUBJECT, String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            DocumentContext json = JsonPath.parse(response.getBody());
            assertThat(json.read("$.subject.metadata.name.value", String.class)).isEqualTo("Test Token Minimal");
            assertThat(json.read("$.subject.metadata.description.value", String.class))
                    .isEqualTo("A minimal test token with only required properties");
            assertThat(json.read("$.subject.metadata.ticker", Object.class)).isNull();
        }

        @Test
        void defaultQueryPriority_returnsCip68ThenCip26() {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    API_BASE_URL + "/api/v2/subjects/" + FULL_TOKEN_SUBJECT, String.class);

            DocumentContext json = JsonPath.parse(response.getBody());
            assertThat(json.read("$.queryPriority[0]", String.class)).isEqualTo("CIP_68");
            assertThat(json.read("$.queryPriority[1]", String.class)).isEqualTo("CIP_26");
        }
    }

    @Nested
    @DisplayName("V2 - Property filter")
    class V2PropertyFilter {

        @Test
        void requiredPlusOptionalProperties_returnsRequested() {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    API_BASE_URL + "/api/v2/subjects/" + FULL_TOKEN_SUBJECT
                            + "?property=name&property=description&property=ticker",
                    String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            DocumentContext json = JsonPath.parse(response.getBody());
            assertThat(json.read("$.subject.metadata.name.value", String.class)).isEqualTo("Test Token Full");
            assertThat(json.read("$.subject.metadata.description.value", String.class))
                    .isEqualTo("A test token with all properties for integration testing");
            assertThat(json.read("$.subject.metadata.ticker.value", String.class)).isEqualTo("TSTF");
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
        void showCipsDetails_returnsCip26StandardBlock() {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    API_BASE_URL + "/api/v2/subjects/" + FULL_TOKEN_SUBJECT
                            + "?show_cips_details=true",
                    String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            DocumentContext json = JsonPath.parse(response.getBody());
            assertThat(json.read("$.subject.standards", Object.class)).isNotNull();
            assertThat(json.read("$.subject.standards.cip26", Object.class)).isNotNull();
            assertThat(json.read("$.subject.standards.cip26.name.value", String.class))
                    .isEqualTo("Test Token Full");
            // CIP-26-only token should not have cip68 standard
            assertThat(json.read("$.subject.standards.cip68", Object.class)).isNull();
        }
    }

    @Nested
    @DisplayName("V2 - Batch query")
    class V2BatchQuery {

        @Test
        void mixedSubjects_returnsOnlyExisting() {
            String body = String.format(
                    "{\"subjects\": [\"%s\", \"%s\"], \"properties\": []}",
                    FULL_TOKEN_SUBJECT, UNKNOWN_SUBJECT);

            ResponseEntity<String> response = postJson(API_BASE_URL + "/api/v2/subjects/query", body);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            DocumentContext json = JsonPath.parse(response.getBody());
            assertThat(json.read("$.subjects", List.class)).hasSize(1);
            assertThat(json.read("$.subjects[0].subject", String.class)).isEqualTo(FULL_TOKEN_SUBJECT);
        }

        @Test
        void allUnknownSubjects_returnsEmptyList() {
            String body = String.format(
                    "{\"subjects\": [\"%s\"], \"properties\": []}", UNKNOWN_SUBJECT);

            ResponseEntity<String> response = postJson(API_BASE_URL + "/api/v2/subjects/query", body);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            DocumentContext json = JsonPath.parse(response.getBody());
            assertThat(json.read("$.subjects", List.class)).isEmpty();
        }

        @Test
        void multipleKnownSubjects_returnsAll() {
            String body = String.format(
                    "{\"subjects\": [\"%s\", \"%s\"], \"properties\": []}",
                    FULL_TOKEN_SUBJECT, MINIMAL_TOKEN_SUBJECT);

            ResponseEntity<String> response = postJson(API_BASE_URL + "/api/v2/subjects/query", body);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            DocumentContext json = JsonPath.parse(response.getBody());
            assertThat(json.read("$.subjects", List.class)).hasSize(2);
        }

        @Test
        void missingRequiredProperties_returnsBadRequest() {
            String body = String.format(
                    "{\"subjects\": [\"%s\"], \"properties\": [\"ticker\"]}", FULL_TOKEN_SUBJECT);

            assertThatThrownBy(() -> postJson(API_BASE_URL + "/api/v2/subjects/query", body))
                    .isInstanceOf(HttpClientErrorException.BadRequest.class);
        }

        @Test
        void shortInvalidSubjects_returnsEmptyWithoutError() {
            String body = "{\"subjects\": [\"nonexistent\", \"abc\", \"42\"], \"properties\": []}";

            ResponseEntity<String> response = postJson(API_BASE_URL + "/api/v2/subjects/query", body);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            DocumentContext json = JsonPath.parse(response.getBody());
            assertThat(json.read("$.subjects", List.class)).isEmpty();
        }

        @Test
        void mixedValidAndInvalidSubjects_returnsOnlyValid() {
            String body = String.format(
                    "{\"subjects\": [\"nonexistent\", \"%s\", \"short\"], \"properties\": []}",
                    FULL_TOKEN_SUBJECT);

            ResponseEntity<String> response = postJson(API_BASE_URL + "/api/v2/subjects/query", body);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            DocumentContext json = JsonPath.parse(response.getBody());
            assertThat(json.read("$.subjects", List.class)).hasSize(1);
            assertThat(json.read("$.subjects[0].subject", String.class)).isEqualTo(FULL_TOKEN_SUBJECT);
        }

        @Test
        void knownSubject_returnsPopulatedMetadata() {
            String body = String.format(
                    "{\"subjects\": [\"%s\"], \"properties\": []}", FULL_TOKEN_SUBJECT);

            ResponseEntity<String> response = postJson(API_BASE_URL + "/api/v2/subjects/query", body);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            DocumentContext json = JsonPath.parse(response.getBody());
            assertThat(json.read("$.subjects", List.class)).hasSize(1);
            assertThat(json.read("$.subjects[0].metadata.name", Object.class)).isNotNull();
            assertThat(json.read("$.subjects[0].metadata.name.value", String.class)).isEqualTo("Test Token Full");
            assertThat(json.read("$.subjects[0].metadata.name.source", String.class)).isNotEmpty();
            assertThat(json.read("$.subjects[0].metadata.description", Object.class)).isNotNull();
            assertThat(json.read("$.subjects[0].metadata.description.value", String.class)).isNotEmpty();
        }

        @Test
        void withPropertyFilter_returnsOnlyRequestedProperties() {
            String body = String.format(
                    "{\"subjects\": [\"%s\"], \"properties\": [\"name\", \"description\", \"ticker\"]}",
                    FULL_TOKEN_SUBJECT);

            ResponseEntity<String> response = postJson(API_BASE_URL + "/api/v2/subjects/query", body);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            DocumentContext json = JsonPath.parse(response.getBody());
            assertThat(json.read("$.subjects", List.class)).hasSize(1);
            assertThat(json.read("$.subjects[0].metadata.name.value", String.class)).isEqualTo("Test Token Full");
            assertThat(json.read("$.subjects[0].metadata.description.value", String.class))
                    .isEqualTo("A test token with all properties for integration testing");
            assertThat(json.read("$.subjects[0].metadata.ticker.value", String.class)).isEqualTo("TSTF");
        }
    }
}
