package org.cardanofoundation.tokenmetadata.registry.api.health;

import com.bloxbean.cardano.yaci.store.core.health.NodeHealthIndicator;
import com.bloxbean.cardano.yaci.store.core.health.NodeSyncIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * yaci-store 2.0.7 health indicators implement the Spring Boot 3
 * {@code org.springframework.boot.actuate.health.HealthIndicator} type. Boot 4 only
 * registers {@link HealthIndicator}. These beans keep the contributor names
 * {@code nodeHealth} and {@code nodeSync} (the {@code HealthIndicator} suffix is stripped)
 * used by the health groups.
 */
@Configuration
public class YaciHealthContributorConfiguration {

    @Bean
    @ConditionalOnBean(NodeHealthIndicator.class)
    public HealthIndicator nodeHealthHealthIndicator(NodeHealthIndicator indicator) {
        return () -> translate(indicator.health());
    }

    @Bean
    @ConditionalOnBean(NodeSyncIndicator.class)
    public HealthIndicator nodeSyncHealthIndicator(NodeSyncIndicator indicator) {
        return () -> translate(indicator.health());
    }

    private static Health translate(org.springframework.boot.actuate.health.Health legacy) {
        Health.Builder builder = Health.status(legacy.getStatus().getCode());
        legacy.getDetails().forEach(builder::withDetail);
        return builder.build();
    }
}
