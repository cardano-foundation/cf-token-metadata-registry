package org.cardanofoundation.tokenmetadata.registry.api;

import jakarta.servlet.DispatcherType;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.filter.ForwardedHeaderFilter;

import java.util.List;

@SpringBootApplication(scanBasePackages = "org.cardanofoundation.tokenmetadata.registry")
public class TokenMetadataRegistryApplication {
    public static void main(String[] args) {
        SpringApplication.run(TokenMetadataRegistryApplication.class, args);
    }

    @Bean
    public FilterRegistrationBean<ForwardedHeaderFilter> forwardedHeaderFilter() {
        final ForwardedHeaderFilter filter = new ForwardedHeaderFilter();
        final FilterRegistrationBean<ForwardedHeaderFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.setUrlPatterns(List.of("/**"));
        return registration;
    }

}
