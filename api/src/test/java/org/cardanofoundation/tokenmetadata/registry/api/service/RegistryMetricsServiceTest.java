package org.cardanofoundation.tokenmetadata.registry.api.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.cardanofoundation.tokenmetadata.registry.model.enums.SyncStatusEnum;
import org.cardanofoundation.tokenmetadata.registry.service.SyncStatus;
import org.cardanofoundation.tokenmetadata.registry.service.TokenMetadataSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegistryMetricsService")
class RegistryMetricsServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private TokenMetadataSyncService tokenMetadataSyncService;

    private MeterRegistry meterRegistry;
    private RegistryMetricsService service;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        service = new RegistryMetricsService(meterRegistry, jdbcTemplate, tokenMetadataSyncService);
        lenient().when(tokenMetadataSyncService.getSyncStatus())
                .thenReturn(SyncStatus.builder().status(SyncStatusEnum.SYNC_DONE).build());
        service.registerMetrics();
    }

    @Nested
    @DisplayName("registerMetrics")
    class RegisterMetrics {

        @Test
        void registersAllGauges() {
            assertThat(meterRegistry.find("cftr.tokens.cip26.count").gauge()).isNotNull();
            assertThat(meterRegistry.find("cftr.tokens.cip68.count").gauge()).isNotNull();
            assertThat(meterRegistry.find("cftr.sync.status").gauge()).isNotNull();
        }

        @Test
        void registersAllCounters() {
            assertThat(meterRegistry.find("cftr.api.queries").tag("version", "v1").counter()).isNotNull();
            assertThat(meterRegistry.find("cftr.api.queries").tag("version", "v2").counter()).isNotNull();
            assertThat(meterRegistry.find("cftr.api.cip.hits").tag("cip", "26").counter()).isNotNull();
            assertThat(meterRegistry.find("cftr.api.cip.hits").tag("cip", "68").counter()).isNotNull();
            assertThat(meterRegistry.find("cftr.api.subjects.queried").counter()).isNotNull();
            assertThat(meterRegistry.find("cftr.api.subjects.not_found").counter()).isNotNull();
        }

        @Test
        void syncStatusGaugeReflectsSyncDone() {
            double value = meterRegistry.find("cftr.sync.status").gauge().value();
            assertThat(value).isEqualTo(2.0);
        }

        @Test
        void syncStatusGaugeReflectsInProgress() {
            when(tokenMetadataSyncService.getSyncStatus())
                    .thenReturn(SyncStatus.builder().status(SyncStatusEnum.SYNC_IN_PROGRESS).build());
            double value = meterRegistry.find("cftr.sync.status").gauge().value();
            assertThat(value).isEqualTo(1.0);
        }

        @Test
        void syncStatusGaugeReflectsError() {
            when(tokenMetadataSyncService.getSyncStatus())
                    .thenReturn(SyncStatus.builder().status(SyncStatusEnum.SYNC_ERROR).build());
            double value = meterRegistry.find("cftr.sync.status").gauge().value();
            assertThat(value).isEqualTo(3.0);
        }
    }

    @Nested
    @DisplayName("recordV1Query")
    class RecordV1Query {

        @Test
        void incrementsV1CounterAndSubjectsQueried() {
            service.recordV1Query(3);

            assertThat(meterRegistry.find("cftr.api.queries").tag("version", "v1").counter().count()).isEqualTo(1.0);
            assertThat(meterRegistry.find("cftr.api.subjects.queried").counter().count()).isEqualTo(3.0);
        }
    }

    @Nested
    @DisplayName("recordV2Query")
    class RecordV2Query {

        @Test
        void incrementsV2CounterAndSubjectsQueried() {
            service.recordV2Query(5);

            assertThat(meterRegistry.find("cftr.api.queries").tag("version", "v2").counter().count()).isEqualTo(1.0);
            assertThat(meterRegistry.find("cftr.api.subjects.queried").counter().count()).isEqualTo(5.0);
        }
    }

    @Nested
    @DisplayName("CIP hit recording")
    class CipHits {

        @Test
        void recordCip26Hit() {
            service.recordCip26Hit();
            assertThat(meterRegistry.find("cftr.api.cip.hits").tag("cip", "26").counter().count()).isEqualTo(1.0);
        }

        @Test
        void recordCip68Hit() {
            service.recordCip68Hit();
            assertThat(meterRegistry.find("cftr.api.cip.hits").tag("cip", "68").counter().count()).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("recordNotFound")
    class RecordNotFound {

        @Test
        void incrementsNotFoundCounter() {
            service.recordNotFound();
            service.recordNotFound();
            assertThat(meterRegistry.find("cftr.api.subjects.not_found").counter().count()).isEqualTo(2.0);
        }
    }

    @Nested
    @DisplayName("refreshTokenCounts")
    class RefreshTokenCounts {

        @Test
        void updatesGaugesFromDatabase() {
            when(jdbcTemplate.queryForObject(eq("SELECT count(*) FROM metadata"), any(Class.class)))
                    .thenReturn(42L);
            when(jdbcTemplate.queryForObject(eq("SELECT count(DISTINCT policy_id || asset_name) FROM metadata_reference_nft"), any(Class.class)))
                    .thenReturn(7L);
            when(jdbcTemplate.queryForObject(eq("SELECT count(DISTINCT key) FROM cip113_registry_node WHERE key <> ''"), any(Class.class)))
                    .thenReturn(5L);

            service.refreshTokenCounts();

            assertThat(meterRegistry.find("cftr.tokens.cip26.count").gauge().value()).isEqualTo(42.0);
            assertThat(meterRegistry.find("cftr.tokens.cip68.count").gauge().value()).isEqualTo(7.0);
            assertThat(meterRegistry.find("cftr.tokens.cip113.count").gauge().value()).isEqualTo(5.0);
        }

        @Test
        void cip113CountExcludesHeadSentinel() {
            // The fix: gauge must use the "WHERE key <> ''" variant so the head-sentinel
            // linked-list marker is not counted as a programmable token. If the SQL regresses
            // to the unfiltered form, this stub will not match and the gauge reads 0.0.
            // lenient(): refreshTokenCounts() calls the metadata and reference-NFT queries
            // before the cip113 one; we don't care what they return here, only that the
            // cip113 SQL is the filtered variant.
            lenient().when(jdbcTemplate.queryForObject(eq("SELECT count(DISTINCT key) FROM cip113_registry_node WHERE key <> ''"), any(Class.class)))
                    .thenReturn(10L);

            service.refreshTokenCounts();

            assertThat(meterRegistry.find("cftr.tokens.cip113.count").gauge().value()).isEqualTo(10.0);
        }

        @Test
        void handlesNullQueryResults() {
            when(jdbcTemplate.queryForObject(eq("SELECT count(*) FROM metadata"), any(Class.class)))
                    .thenReturn(null);
            when(jdbcTemplate.queryForObject(eq("SELECT count(DISTINCT policy_id || asset_name) FROM metadata_reference_nft"), any(Class.class)))
                    .thenReturn(null);

            service.refreshTokenCounts();

            assertThat(meterRegistry.find("cftr.tokens.cip26.count").gauge().value()).isEqualTo(0.0);
            assertThat(meterRegistry.find("cftr.tokens.cip68.count").gauge().value()).isEqualTo(0.0);
        }

        @Test
        void handlesExceptionGracefully() {
            when(jdbcTemplate.queryForObject(eq("SELECT count(*) FROM metadata"), any(Class.class)))
                    .thenThrow(new RuntimeException("DB down"));

            service.refreshTokenCounts();

            assertThat(meterRegistry.find("cftr.tokens.cip26.count").gauge().value()).isEqualTo(0.0);
        }
    }

}
