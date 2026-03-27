package org.cardanofoundation.tokenmetadata.registry.it;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for health and sync status endpoints.
 * Verifies startup, liveness, readiness probes and legacy health endpoint.
 */
public class SyncStatusIntegrationIT extends BaseIntegrationIT {

    private static final String KNOWN_SUBJECT = "a0b1c2d3e4f5a0b1c2d3e4f5a0b1c2d3e4f5a0b1c2d3e4f5a0b1c2d354455354544f4b454e";

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
        void afterSync_reportsSyncedTrue() {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    API_BASE_URL + "/health", String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            DocumentContext json = JsonPath.parse(response.getBody());
            assertThat(json.read("$.synced", Boolean.class)).isTrue();
            assertThat(json.read("$.syncStatus", String.class)).contains("offchain:");
            assertThat(json.read("$.syncStatus", String.class)).contains("onchain:");
        }
    }

    @Nested
    @DisplayName("Actuator health")
    class ActuatorHealth {

        @Test
        void shouldBeUp() {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    API_BASE_URL + "/actuator/health", String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            DocumentContext json = JsonPath.parse(response.getBody());
            assertThat(json.read("$.status", String.class)).isEqualTo("UP");
        }

        @Test
        void shouldExposeAllGroups() {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    API_BASE_URL + "/actuator/health", String.class);

            DocumentContext json = JsonPath.parse(response.getBody());
            List<String> groups = json.read("$.groups", List.class);
            assertThat(groups).isNotNull()
                    .contains("startup")
                    .contains("liveness")
                    .contains("readiness");
        }
    }

    @Nested
    @DisplayName("Startup probe")
    class StartupProbe {

        @Test
        void shouldBeUp_whenInitialized() {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    API_BASE_URL + "/actuator/health/startup", String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            DocumentContext json = JsonPath.parse(response.getBody());
            assertThat(json.read("$.status", String.class)).isEqualTo("UP");
        }

        @Test
        void shouldIncludeDbComponent() {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    API_BASE_URL + "/actuator/health/startup", String.class);

            DocumentContext json = JsonPath.parse(response.getBody());
            assertThat(json.read("$.components.db", Object.class)).isNotNull();
            assertThat(json.read("$.components.db.status", String.class)).isEqualTo("UP");
        }

        @Test
        void shouldIncludeOnchainConnectionComponent() {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    API_BASE_URL + "/actuator/health/startup", String.class);

            DocumentContext json = JsonPath.parse(response.getBody());
            assertThat(json.read("$.components.onchainConnection", Object.class)).isNotNull();
            assertThat(json.read("$.components.onchainConnection.status", String.class)).isEqualTo("UP");
            assertThat(json.read("$.components.onchainConnection.details.connectionAlive", Boolean.class)).isTrue();
            assertThat(json.read("$.components.onchainConnection.details.receivingBlocks", Boolean.class)).isTrue();
        }
    }

    @Nested
    @DisplayName("Liveness probe")
    class LivenessProbe {

        @Test
        void shouldBeUp() {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    API_BASE_URL + "/actuator/health/liveness", String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            DocumentContext json = JsonPath.parse(response.getBody());
            assertThat(json.read("$.status", String.class)).isEqualTo("UP");
        }

        @Test
        void shouldIncludeSyncIndicators() {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    API_BASE_URL + "/actuator/health/liveness", String.class);

            DocumentContext json = JsonPath.parse(response.getBody());
            assertThat(json.read("$.components.livenessState", Object.class)).isNotNull();
            assertThat(json.read("$.components.offchainSync", Object.class)).isNotNull();
            assertThat(json.read("$.components.onchainConnection", Object.class)).isNotNull();
        }
    }

    @Nested
    @DisplayName("Readiness probe")
    class ReadinessProbe {

        @Test
        void shouldBeUp_whenSynced() {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    API_BASE_URL + "/actuator/health/readiness", String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            DocumentContext json = JsonPath.parse(response.getBody());
            assertThat(json.read("$.status", String.class)).isEqualTo("UP");
        }

        @Test
        void shouldIncludeOffchainSync() {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    API_BASE_URL + "/actuator/health/readiness", String.class);

            DocumentContext json = JsonPath.parse(response.getBody());
            assertThat(json.read("$.components.offchainSync", Object.class)).isNotNull();
            assertThat(json.read("$.components.offchainSync.status", String.class)).isEqualTo("UP");
        }

        @Test
        void shouldIncludeOnchainSync() {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    API_BASE_URL + "/actuator/health/readiness", String.class);

            DocumentContext json = JsonPath.parse(response.getBody());
            assertThat(json.read("$.components.onchainReadiness", Object.class)).isNotNull();
            assertThat(json.read("$.components.onchainReadiness.status", String.class)).isEqualTo("UP");
            assertThat(json.read("$.components.onchainReadiness.details.syncStatus", String.class)).isEqualTo("Synced");
            assertThat(json.read("$.components.onchainReadiness.details.syncPercentage", Object.class)).isNotNull();
        }

        @Test
        void shouldIncludeDb() {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    API_BASE_URL + "/actuator/health/readiness", String.class);

            DocumentContext json = JsonPath.parse(response.getBody());
            assertThat(json.read("$.components.db", Object.class)).isNotNull();
            assertThat(json.read("$.components.db.status", String.class)).isEqualTo("UP");
        }
    }
}
