package org.cardanofoundation.metadatatools.metafides.config;

import org.cardanofoundation.metadatatools.metafides.model.OAuth2GrantType;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.config.WebFluxConfigurer;


@Configuration
public class SpringWebConfig implements WebFluxConfigurer {
    @Override
    public void addFormatters(final FormatterRegistry registry) {
        registry.addConverter(new Converter<String, OAuth2GrantType>() {
            @Override
            public OAuth2GrantType convert(@NonNull final String source) {
                return OAuth2GrantType.valueOf(source.toUpperCase());
            }
        });
    }
}
