package org.cardanofoundation.tokenmetadata.registry.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
@EnableWebSecurity
public class SpringWebSecurityConfig {
  @Bean
  public SecurityFilterChain filterChain(final HttpSecurity http) throws Exception {
    http.csrf((csrf) -> csrf.disable())
        .authorizeHttpRequests((matcherRegistry) -> matcherRegistry.anyRequest().permitAll())
        .headers(
            (headers) ->
                headers
                    .contentSecurityPolicy(
                        (policy) ->
                            policy.policyDirectives(
                                "default-src 'self'; script-src 'self' 'unsafe-inline'; script-src-elem 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; style-src-elem 'self' 'unsafe-inline'; img-src 'self' 'unsafe-inline'"))
                    .referrerPolicy(
                        (policy) ->
                            policy.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
                    .permissionsPolicy((policy) -> policy.policy("geolocation=(self)")));

    return http.build();
  }
}
