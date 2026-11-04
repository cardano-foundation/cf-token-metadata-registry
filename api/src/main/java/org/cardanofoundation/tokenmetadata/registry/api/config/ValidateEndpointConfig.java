package org.cardanofoundation.tokenmetadata.registry.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * The guards of the public validate endpoint (ADR-023): a bound on the request size, and CORS for the website that
 * calls it from the browser. Only {@code /api/v2/validate/**} is affected.
 */
@Configuration(proxyBeanMethods = false)
public class ValidateEndpointConfig {

    static final String VALIDATE_PATHS = "/api/v2/validate/**";

    /**
     * CORS for the validate endpoint only, from {@code registry.validate.cors.allowed-origins} (comma separated, e.g. the
     * GitHub Pages site). Without origins, browsers on other sites cannot call it.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${registry.validate.cors.allowed-origins:}") String allowedOrigins) {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        List<String> origins = Arrays.stream(allowedOrigins.split(",")).map(String::trim).filter(o -> !o.isEmpty()).toList();
        if (!origins.isEmpty()) {
            CorsConfiguration cors = new CorsConfiguration();
            cors.setAllowedOrigins(origins);
            cors.setAllowedMethods(List.of("POST", "OPTIONS"));
            cors.setAllowedHeaders(List.of("Content-Type"));
            cors.setMaxAge(3600L);
            source.registerCorsConfiguration(VALIDATE_PATHS, cors);
        }
        return source;
    }

    /** Rejects a validate request larger than {@code registry.validate.max-request-bytes}, or of unknown length. */
    @Bean
    public FilterRegistrationBean<RequestSizeFilter> validateRequestSizeFilter(
            @Value("${registry.validate.max-request-bytes:65536}") long maxRequestBytes) {
        FilterRegistrationBean<RequestSizeFilter> registration = new FilterRegistrationBean<>(new RequestSizeFilter(maxRequestBytes));
        registration.addUrlPatterns("/api/v2/validate/*");
        return registration;
    }

    static final class RequestSizeFilter extends OncePerRequestFilter {

        private final long maxRequestBytes;

        RequestSizeFilter(long maxRequestBytes) {
            this.maxRequestBytes = maxRequestBytes;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            if ("POST".equalsIgnoreCase(request.getMethod())) {
                long length = request.getContentLengthLong();
                if (length < 0) {
                    response.sendError(HttpStatus.LENGTH_REQUIRED.value(), "Content-Length is required");
                    return;
                }
                if (length > maxRequestBytes) {
                    response.sendError(HttpStatus.PAYLOAD_TOO_LARGE.value(),
                            "the request is larger than " + maxRequestBytes + " bytes");
                    return;
                }
            }
            chain.doFilter(request, response);
        }
    }
}
