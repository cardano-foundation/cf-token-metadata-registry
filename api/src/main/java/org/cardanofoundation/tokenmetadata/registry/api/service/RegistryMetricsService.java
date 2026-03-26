package org.cardanofoundation.tokenmetadata.registry.api.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.model.enums.SyncStatusEnum;
import org.cardanofoundation.tokenmetadata.registry.service.TokenMetadataSyncService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
@RequiredArgsConstructor
public class RegistryMetricsService {

    private static final String NAMESPACE = "cftr";
    private static final String CIP_HITS_METRIC = NAMESPACE + ".api.cip.hits";

    private final MeterRegistry meterRegistry;
    private final JdbcTemplate jdbcTemplate;
    private final TokenMetadataSyncService tokenMetadataSyncService;

    private final AtomicLong cip26Count = new AtomicLong(0);
    private final AtomicLong cip68Count = new AtomicLong(0);
    private final AtomicLong cip113Count = new AtomicLong(0);

    private Counter v1QueryCounter;
    private Counter v2QueryCounter;
    private Counter v2Cip26HitCounter;
    private Counter v2Cip68HitCounter;
    private Counter v2Cip113HitCounter;
    private Counter subjectsQueriedCounter;
    private Counter notFoundCounter;

    @PostConstruct
    void registerMetrics() {
        Gauge.builder(NAMESPACE + ".tokens.cip26.count", cip26Count, AtomicLong::doubleValue)
                .description("Total number of CIP-26 tokens in the registry")
                .register(meterRegistry);

        Gauge.builder(NAMESPACE + ".tokens.cip68.count", cip68Count, AtomicLong::doubleValue)
                .description("Total number of CIP-68 reference NFTs in the registry")
                .register(meterRegistry);

        Gauge.builder(NAMESPACE + ".tokens.cip113.count", cip113Count, AtomicLong::doubleValue)
                .description("Total number of CIP-113 programmable tokens in the registry")
                .register(meterRegistry);

        Gauge.builder(NAMESPACE + ".sync.status", this, RegistryMetricsService::getSyncStatusValue)
                .description("Sync status: 0=not_started, 1=in_progress, 2=done, 3=error, 4=external_job")
                .register(meterRegistry);

        v1QueryCounter = Counter.builder(NAMESPACE + ".api.queries")
                .tag("version", "v1")
                .description("Number of V1 API queries")
                .register(meterRegistry);

        v2QueryCounter = Counter.builder(NAMESPACE + ".api.queries")
                .tag("version", "v2")
                .description("Number of V2 API queries")
                .register(meterRegistry);

        v2Cip26HitCounter = Counter.builder(CIP_HITS_METRIC)
                .tag("cip", "26")
                .description("Number of V2 queries resolved via CIP-26")
                .register(meterRegistry);

        v2Cip68HitCounter = Counter.builder(CIP_HITS_METRIC)
                .tag("cip", "68")
                .description("Number of V2 queries resolved via CIP-68")
                .register(meterRegistry);

        v2Cip113HitCounter = Counter.builder(CIP_HITS_METRIC)
                .tag("cip", "113")
                .description("Number of V2 queries enriched with CIP-113 data")
                .register(meterRegistry);

        subjectsQueriedCounter = Counter.builder(NAMESPACE + ".api.subjects.queried")
                .description("Total number of subjects looked up")
                .register(meterRegistry);

        notFoundCounter = Counter.builder(NAMESPACE + ".api.subjects.not_found")
                .description("Number of subject lookups that returned no result")
                .register(meterRegistry);
    }

    @Scheduled(fixedRate = 30_000, initialDelay = 5_000)
    void refreshTokenCounts() {
        try {
            Long metadataCount = jdbcTemplate.queryForObject("SELECT count(*) FROM metadata", Long.class);
            cip26Count.set(metadataCount != null ? metadataCount : 0);

            Long refNftCount = jdbcTemplate.queryForObject(
                    "SELECT count(DISTINCT policy_id || asset_name) FROM metadata_reference_nft", Long.class);
            cip68Count.set(refNftCount != null ? refNftCount : 0);

            Long cip113TokenCount = jdbcTemplate.queryForObject(
                    "SELECT count(DISTINCT policy_id) FROM cip113_registry_node", Long.class);
            cip113Count.set(cip113TokenCount != null ? cip113TokenCount : 0);
        } catch (Exception e) {
            log.warn("Failed to refresh token counts for metrics: {}", e.getMessage());
        }
    }

    public void recordV1Query(int subjectCount) {
        v1QueryCounter.increment();
        subjectsQueriedCounter.increment(subjectCount);
    }

    public void recordV2Query(int subjectCount) {
        v2QueryCounter.increment();
        subjectsQueriedCounter.increment(subjectCount);
    }

    public void recordCip26Hit() {
        v2Cip26HitCounter.increment();
    }

    public void recordCip68Hit() {
        v2Cip68HitCounter.increment();
    }

    public void recordCip113Hit() {
        v2Cip113HitCounter.increment();
    }

    public void recordNotFound() {
        notFoundCounter.increment();
    }

    private double getSyncStatusValue() {
        SyncStatusEnum status = tokenMetadataSyncService.getSyncStatus().getStatus();
        return switch (status) {
            case SYNC_NOT_STARTED -> 0;
            case SYNC_IN_PROGRESS -> 1;
            case SYNC_DONE -> 2;
            case SYNC_ERROR -> 3;
            case SYNC_IN_EXTRA_JOB -> 4;
        };
    }

}
