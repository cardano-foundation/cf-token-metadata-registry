package org.cardanofoundation.tokenmetadata.registry.config;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JsonConfiguration {

    @Bean
    public JsonMapper objectMapper() {
        // Jackson 3 mappers are immutable. FAIL_ON_UNKNOWN_PROPERTIES is already off by default;
        // disabling it here keeps the previous ObjectMapper configuration explicit.
        return JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

}
