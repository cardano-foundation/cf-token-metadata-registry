package org.cardanofoundation.metadatatools.metafides;

import org.cardanofoundation.metadatatools.metafides.data.OAuth2GrantType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SpringWebConfig implements WebMvcConfigurer {

    @Autowired
    AuthenticationInterceptor authenticationInterceptor;

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(authenticationInterceptor);
    }

    @Override
    //(Converter<String, OAuth2GrantType>) source -> OAuth2GrantType.valueOf(source.toUpperCase())
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new Converter<String, OAuth2GrantType>() {
            @Override
            public OAuth2GrantType convert(final String source) {
                return OAuth2GrantType.valueOf(source.toUpperCase());
            }
        });
    }
}
