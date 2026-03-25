package org.cardanofoundation.tokenmetadata.registry.it;

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
 * Integration tests for health and sync status endpoints.
 * Verifies startup, liveness, readiness probes and legacy health endpoint.
 */
public class SyncStatusIntegrationIT extends BaseIntegrationIT {

    private static final String KNOWN_SUBJECT = "a0b1c2d3e4f5a0b1c2d3e4f5a0b1c2d3e4f5a0b1c2d3e4f5a0b1c2d354455354544f4b454e";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    static void setUp() {
        waitForApiReady();
        waitForSyncComplete();
    }

    private static void waitForSyncComplete() {
        log.info("Waiting for sync to complete before running sync status tests ...");
        await().atMost(Duration.ofMinutes(5))
                .pollInterval(Duration.ofSeconds(2))
                .ignoreExceptions()
                .until(() -> {
                    ResponseEntity<String> response = restTemplate.getForEntity(
                            API_BASE_URL + "/metadata/" + KNOWN_SUBJECT, String.class);
                    log.info("Sync status test - sync poll: status={}", response.getStatusCode());
                    return response.getStatusCode() == HttpStatus.OK;
                });
        log.info("Sync complete, running sync status tests.");
    }

    @Nested
    @DisplayName("Legacy health endpoint (deprecated)")
    class HealthEndpoint {

        @Test
        void afterSync_reportsSyncedTrue() throws Exception {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    API_BASE_URL + "/health", String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            JsonNode json = objectMapper.readTree(response.getBody());
            assertThat(json.get("synced").asBoolean()).isTrue();
            assertThat(json.get("syncStatus").asText()).contains("offchain:");
            assertThat(json.get("syncStatus").asText()).contains("onchain:");
        }
    }

    @Nested
    @DisplayName("Actuator health")
    class ActuatorHealth {

        @Test
        void shouldBeUp() throws Exception {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    API_BASE_URL + "/actuator/health", String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            JsonNode json = objectMapper.readTree(response.getBody());
            assertThat(json.get("status").asText()).isEqualTo("UP");
        }

        @Test
        void shouldExposeAllGroups() throws Exception {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    API_BASE_URL + "/actuator/health", String.class);

            JsonNode json = objectMapper.readTree(response.getBody());
            JsonNode groups = json.get("groups");
            assertThat(groups).isNotNull();
            assertThat(groups.toString()).contains("startup");
            assertThat(groups.toString()).contains("liveness");
            assertThat(groups.toString()).contains("readiness");
        }
    }

    @Nested
    @DisplayName("Startup probe")
    class StartupProbe {

        @Test
        void shouldBeUp_whenInitialized() throws Exception {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    API_BASE_URL + "/actuator/health/startup", String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            JsonNode json = objectMapper.readTree(response.getBody());
            assertThat(json.get("status").asText()).isEqualTo("UP");
        }

        @Test
        void shouldIncludeDbComponent() throws Exception {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    API_BASE_URL + "/actuator/health/startup", String.class);

            JsonNode components = objectMapper.readTree(response.getBody()).get("components");
            assertThat(components.get("db")).isNotNull();
            assertThat(components.get("db").get("status").asText()).isEqualTo("UP");
        }

        @Test
        void shouldIncludeOnchainConnectionComponent() throws Exception {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    API_BASE_URL + "/actuator/health/startup", String.class);

            JsonNode components = objectMapper.readTree(response.getBody()).get("components");
            assertThat(components.get("onchainConnection")).isNotNull();
            assertThat(components.get("onchainConnection").get("status").asText()).isEqualTo("UP");
            assertThat(components.get("onchainConnection").get("details").get("connectionAlive").asBoolean()).isTrue();
            assertThat(components.get("onchainConnection").get("details").get("receivingBlocks").asBoolean()).isTrue();
        }
    }

    @Nested
    @DisplayName("Liveness probe")
    class LivenessProbe {

        @Test
        void shouldBeUp() throws Exception {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    API_BASE_URL + "/actuator/health/liveness", String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            JsonNode json = objectMapper.readTree(response.getBody());
            assertThat(json.get("status").asText()).isEqualTo("UP");
        }

        @Test
        void shouldIncludeSyncIndicators() throws Exception {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    API_BASE_URL + "/actuator/health/liveness", String.class);

            JsonNode components = objectMapper.readTree(response.getBody()).get("components");
            assertThat(components.get("livenessState")).isNotNull();
            assertThat(components.get("offchainSync")).isNotNull();
            assertThat(components.get("onchainConnection")).isNotNull();
        }
    }

    @Nested
    @DisplayName("Readiness probe")
    class ReadinessProbe {

        @Test
        void shouldBeUp_whenSynced() throws Exception {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    API_BASE_URL + "/actuator/health/readiness", String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            JsonNode json = objectMapper.readTree(response.getBody());
            assertThat(json.get("status").asText()).isEqualTo("UP");
        }

        @Test
        void shouldIncludeOffchainSync() throws Exception {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    API_BASE_URL + "/actuator/health/readiness", String.class);

            JsonNode components = objectMapper.readTree(response.getBody()).get("components");
            assertThat(components.get("offchainSync")).isNotNull();
            assertThat(components.get("offchainSync").get("status").asText()).isEqualTo("UP");
        }

        @Test
        void shouldIncludeOnchainSync() throws Exception {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    API_BASE_URL + "/actuator/health/readiness", String.class);

            JsonNode components = objectMapper.readTree(response.getBody()).get("components");
            assertThat(components.get("onchainReadiness")).isNotNull();
            assertThat(components.get("onchainReadiness").get("status").asText()).isEqualTo("UP");
            assertThat(components.get("onchainReadiness").get("details").get("syncStatus").asText()).isEqualTo("Synced");
            assertThat(components.get("onchainReadiness").get("details").get("syncPercentage")).isNotNull();
        }

        @Test
        void shouldIncludeDb() throws Exception {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    API_BASE_URL + "/actuator/health/readiness", String.class);

            JsonNode components = objectMapper.readTree(response.getBody()).get("components");
            assertThat(components.get("db")).isNotNull();
            assertThat(components.get("db").get("status").asText()).isEqualTo("UP");
        }
    }
}
