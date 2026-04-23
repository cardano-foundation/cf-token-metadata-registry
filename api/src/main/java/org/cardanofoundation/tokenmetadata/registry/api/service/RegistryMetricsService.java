package org.cardanofoundation.tokenmetadata.registry.api.service;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.Metadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.Property;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.Subject;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.model.ProgrammableTokenCip113;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

/**
 * Emits per-CIP hit counters for V2 subject queries. Nothing else — token counts,
 * API-query-by-version, sync status etc. are covered by Spring Boot's
 * {@code http_server_requests} and yaci-store's {@code yaci.store.*} gauges.
 */
@Service
public class RegistryMetricsService {

    private static final String CIP_HITS_METRIC = "cftr.api.cip.hits";
    private static final String SOURCE_CIP_26 = "CIP_26";
    private static final String SOURCE_CIP_68 = "CIP_68";

    private final Counter cip26Hits;
    private final Counter cip68Hits;
    private final Counter cip113Hits;

    public RegistryMetricsService(MeterRegistry meterRegistry) {
        this.cip26Hits = Counter.builder(CIP_HITS_METRIC)
                .tag("cip", "26")
                .description("V2 queries resolved via CIP-26 offchain metadata")
                .register(meterRegistry);
        this.cip68Hits = Counter.builder(CIP_HITS_METRIC)
                .tag("cip", "68")
                .description("V2 queries resolved via CIP-68 on-chain metadata")
                .register(meterRegistry);
        this.cip113Hits = Counter.builder(CIP_HITS_METRIC)
                .tag("cip", "113")
                .description("V2 queries enriched with a CIP-113 extension")
                .register(meterRegistry);
    }

    public void recordSubject(Subject subject) {
        Set<String> sources = collectSources(subject.metadata());
        if (sources.contains(SOURCE_CIP_26)) {
            cip26Hits.increment();
        }
        if (sources.contains(SOURCE_CIP_68)) {
            cip68Hits.increment();
        }
        if (subject.extensions() != null
                && subject.extensions().containsKey(ProgrammableTokenCip113.EXTENSION_KEY)) {
            cip113Hits.increment();
        }
    }

    private static Set<String> collectSources(Metadata metadata) {
        Set<String> sources = new HashSet<>(4);
        addSource(sources, metadata.name());
        addSource(sources, metadata.description());
        addSource(sources, metadata.ticker());
        addSource(sources, metadata.decimals());
        addSource(sources, metadata.logo());
        addSource(sources, metadata.url());
        addSource(sources, metadata.version());
        return sources;
    }

    private static void addSource(Set<String> sources, Property<?> property) {
        if (property != null && property.source() != null) {
            sources.add(property.source());
        }
    }

}
