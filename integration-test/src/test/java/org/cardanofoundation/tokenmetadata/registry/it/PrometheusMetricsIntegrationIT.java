package org.cardanofoundation.tokenmetadata.registry.it;

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
 * Integration tests for Prometheus metrics endpoint.
 * Verifies that /actuator/prometheus is available and exposes
 * custom application metrics as well as standard JVM/Spring metrics.
 */
public class PrometheusMetricsIntegrationIT extends BaseIntegrationIT {

    private static final String KNOWN_SUBJECT = "a0b1c2d3e4f5a0b1c2d3e4f5a0b1c2d3e4f5a0b1c2d3e4f5a0b1c2d354455354544f4b454e";
    private static final String UNKNOWN_SUBJECT = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffff556e6b6e6f776e";

    private static String prometheusBody;

    @BeforeAll
    static void setUp() {
        waitForApiReady();
        waitForSyncComplete();
        triggerApiQueries();
        prometheusBody = fetchPrometheusMetrics();
    }

    private static void waitForSyncComplete() {
        log.info("Waiting for sync to complete before running metrics tests ...");
        await().atMost(Duration.ofMinutes(5))
                .pollInterval(Duration.ofSeconds(2))
                .ignoreExceptions()
                .until(() -> {
                    ResponseEntity<String> response = restTemplate.getForEntity(
                            API_BASE_URL + "/metadata/" + KNOWN_SUBJECT, String.class);
                    return response.getStatusCode() == HttpStatus.OK;
                });
        log.info("Sync complete.");
    }

    private static void triggerApiQueries() {
        // Fire a few API requests so counters are non-zero
        restTemplate.getForEntity(API_BASE_URL + "/metadata/" + KNOWN_SUBJECT, String.class);
        try {
            restTemplate.getForEntity(API_BASE_URL + "/api/v2/subjects/" + KNOWN_SUBJECT, String.class);
        } catch (Exception _) {
            // V2 query may throw on 404 — expected for metrics population
        }
        try {
            restTemplate.getForEntity(API_BASE_URL + "/api/v2/subjects/" + UNKNOWN_SUBJECT, String.class);
        } catch (Exception _) {
            // Unknown subject returns 404 — expected for not-found counter
        }
    }

    private static String fetchPrometheusMetrics() {
        // Allow gauge refresh (30s cache) to pick up DB counts
        await().atMost(Duration.ofSeconds(45))
                .pollInterval(Duration.ofSeconds(5))
                .ignoreExceptions()
                .until(() -> {
                    ResponseEntity<String> response = restTemplate.getForEntity(
                            API_BASE_URL + "/actuator/prometheus", String.class);
                    return response.getStatusCode() == HttpStatus.OK
                            && response.getBody() != null
                            && response.getBody().contains("cftr_tokens_cip26_count");
                });
        ResponseEntity<String> response = restTemplate.getForEntity(
                API_BASE_URL + "/actuator/prometheus", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        log.info("Prometheus endpoint returned {} bytes", response.getBody().length());
        return response.getBody();
    }

    @Nested
    @DisplayName("Endpoint availability")
    class EndpointAvailability {

        @Test
        void prometheusEndpointReturns200() {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    API_BASE_URL + "/actuator/prometheus", String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Custom application metrics (cftr.*)")
    class CustomMetrics {

        @Test
        void cip26TokenCountGaugeIsPresent() {
            assertThat(prometheusBody).contains("cftr_tokens_cip26_count");
            assertMetricHasPositiveValue("cftr_tokens_cip26_count");
        }

        @Test
        void cip68TokenCountGaugeIsPresent() {
            assertThat(prometheusBody).contains("cftr_tokens_cip68_count");
        }

        @Test
        void cip113TokenCountGaugeIsPresent() {
            assertThat(prometheusBody).contains("cftr_tokens_cip113_count");
        }

        @Test
        void syncStatusGaugeIsPresent() {
            assertThat(prometheusBody).contains("cftr_sync_status");
        }

        @Test
        void syncStatusIndicatesDone() {
            // After sync completes, status should be 2.0 (SYNC_DONE)
            assertMetricHasValue("cftr_sync_status", 2.0);
        }

        @Test
        void apiQueryCountersArePresent() {
            assertThat(prometheusBody).contains("cftr_api_queries_total");
        }

        @Test
        void v1QueryCounterIsIncremented() {
            assertMetricLineContaining("cftr_api_queries_total", "version=\"v1\"", value -> value > 0);
        }

        @Test
        void v2QueryCounterIsIncremented() {
            assertMetricLineContaining("cftr_api_queries_total", "version=\"v2\"", value -> value > 0);
        }

        @Test
        void subjectsQueriedCounterIsPresent() {
            assertThat(prometheusBody).contains("cftr_api_subjects_queried_total");
            assertMetricHasPositiveValue("cftr_api_subjects_queried_total");
        }

        @Test
        void notFoundCounterIsPresent() {
            assertThat(prometheusBody).contains("cftr_api_subjects_not_found_total");
            assertMetricHasPositiveValue("cftr_api_subjects_not_found_total");
        }

        @Test
        void cipHitCountersArePresent() {
            assertThat(prometheusBody).contains("cftr_api_cip_hits_total");
        }
    }

    @Nested
    @DisplayName("JVM metrics (auto-configured)")
    class JvmMetrics {

        @Test
        void jvmMemoryMetricsArePresent() {
            assertThat(prometheusBody).contains("jvm_memory_used_bytes");
        }

        @Test
        void jvmThreadMetricsArePresent() {
            assertThat(prometheusBody).contains("jvm_threads_live_threads");
        }

        @Test
        void jvmGcMetricsArePresent() {
            assertThat(prometheusBody).contains("jvm_gc_");
        }
    }

    @Nested
    @DisplayName("Spring Boot HTTP metrics")
    class HttpMetrics {

        @Test
        void httpServerRequestMetricsArePresent() {
            assertThat(prometheusBody).contains("http_server_requests");
        }
    }

    @Nested
    @DisplayName("Application tag")
    class ApplicationTag {

        @Test
        void allMetricsIncludeApplicationTag() {
            assertThat(prometheusBody).contains("application=\"cf-token-metadata-registry\"");
        }
    }

    // --- helpers ---

    private void assertMetricHasPositiveValue(String metricName) {
        prometheusBody.lines()
                .filter(line -> line.startsWith(metricName) && !line.startsWith(metricName + "_"))
                .filter(line -> !line.startsWith("#"))
                .findFirst()
                .ifPresentOrElse(
                        line -> {
                            double value = extractValue(line);
                            assertThat(value).as("Metric %s should be > 0", metricName).isGreaterThan(0);
                        },
                        () -> {
                            // metric might have labels — look for any line containing the metric name
                            boolean found = prometheusBody.lines()
                                    .filter(line -> !line.startsWith("#"))
                                    .filter(line -> line.contains(metricName))
                                    .anyMatch(line -> extractValue(line) > 0);
                            assertThat(found).as("Metric %s should have at least one line > 0", metricName).isTrue();
                        }
                );
    }

    private void assertMetricHasValue(String metricName, double expected) {
        boolean found = prometheusBody.lines()
                .filter(line -> !line.startsWith("#"))
                .filter(line -> line.contains(metricName))
                .anyMatch(line -> extractValue(line) == expected);
        assertThat(found).as("Metric %s should have value %s", metricName, expected).isTrue();
    }

    private void assertMetricLineContaining(String metricName, String label, java.util.function.DoublePredicate valuePredicate) {
        boolean found = prometheusBody.lines()
                .filter(line -> !line.startsWith("#"))
                .filter(line -> line.contains(metricName) && line.contains(label))
                .anyMatch(line -> valuePredicate.test(extractValue(line)));
        assertThat(found).as("Metric %s with label %s should match predicate", metricName, label).isTrue();
    }

    private static double extractValue(String prometheusLine) {
        String[] parts = prometheusLine.trim().split("\\s+");
        if (parts.length >= 2) {
            try {
                return Double.parseDouble(parts[parts.length - 1]);
            } catch (NumberFormatException _) {
                return 0;
            }
        }
        return 0;
    }
}
