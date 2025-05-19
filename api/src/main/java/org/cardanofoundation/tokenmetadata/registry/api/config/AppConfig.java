package org.cardanofoundation.tokenmetadata.registry.api.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.api.model.QueryPriority;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableScheduling
@Slf4j
public class AppConfig {

    @Component
    @Getter
    @Setter
    public static class CipPriorityConfiguration {

        @Value("${cip.query.priority}")
        private List<QueryPriority> defaultPriority;

        @PostConstruct
        public void init() {
            var priority = defaultPriority.stream().map(QueryPriority::name).collect(Collectors.joining(","));
            log.info("INIT - CIP priority, higher to lower: {}", priority);
        }

    }

}
