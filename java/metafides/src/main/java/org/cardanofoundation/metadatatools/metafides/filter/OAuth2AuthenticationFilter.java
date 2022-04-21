package org.cardanofoundation.metadatatools.metafides.filter;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import lombok.extern.log4j.Log4j2;
import org.cardanofoundation.metadatatools.metafides.config.OAuth2Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotNull;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;


@Log4j2
@Component
public class OAuth2AuthenticationFilter implements WebFilter {

    @Autowired
    OAuth2Config oAuth2Config;

    private Mono<Void> verifyBearerToken(final String token, @NotNull final ServerWebExchange serverWebExchange, @NonNull final WebFilterChain webFilterChain) {
        try {
            final ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
            final JWKSource<SecurityContext> keySource = new RemoteJWKSet<>(new URL(oAuth2Config.getKeyStoreUri()));
            final JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keySource);
            jwtProcessor.setJWSKeySelector(keySelector);
            jwtProcessor.setJWTClaimsSetVerifier(new DefaultJWTClaimsVerifier<>(
                    new JWTClaimsSet.Builder().issuer(oAuth2Config.getIssuerUri()).build(),
                    new HashSet<>(Arrays.asList("token_use", "scope", "exp", "client_id"))));
            final JWTClaimsSet claimsSet = jwtProcessor.process(token, null);
            final String clientId = claimsSet.getStringClaim("client_id");
            final String[] scopesClaim = claimsSet.getStringClaim("scope").split(" ");
            if (claimsSet.getStringClaim("token_use").equalsIgnoreCase("access") &&
                    scopesClaim.length > 0 &&
                    Arrays.stream(scopesClaim).anyMatch(serverWebExchange.getRequest().getURI().toString()::startsWith)) {
                serverWebExchange.getAttributes().put("clientId", clientId);
                return webFilterChain.filter(serverWebExchange);
            } else {
                serverWebExchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return serverWebExchange.getResponse().setComplete();
            }
        } catch (final MalformedURLException e) {
            log.error("Wrong configuration for JWKS.", e);
            serverWebExchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return serverWebExchange.getResponse().setComplete();
        } catch (final JOSEException | ParseException | BadJOSEException e) {
            log.error("Invalid access token provided with request.", e);
            serverWebExchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return serverWebExchange.getResponse().setComplete();
        }
    }

    @Override
    @NonNull
    public Mono<Void> filter(@NotNull final ServerWebExchange serverWebExchange, @NonNull final WebFilterChain webFilterChain) {
        final String servletPath = serverWebExchange.getRequest().getPath().pathWithinApplication().value();
        if (Arrays.stream(oAuth2Config.getPermissionlessPaths()).anyMatch(servletPath::startsWith)) {
            return webFilterChain.filter(serverWebExchange);
        }

        final List<String> authHeaders = serverWebExchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION);
        if (authHeaders != null && !authHeaders.isEmpty()) {
            final String authHeader = authHeaders.get(0);
            if (authHeader.toLowerCase().startsWith("basic")) {
                if (Arrays.stream(oAuth2Config.getBasicAuthPaths()).anyMatch(servletPath::equalsIgnoreCase)) {
                    serverWebExchange.getAttributes().put("authHeader", authHeader);
                    return webFilterChain.filter(serverWebExchange);
                }
            } else if (authHeader.toLowerCase().startsWith("bearer")) {
                return verifyBearerToken(authHeader.substring("beraer".length()).trim(), serverWebExchange, webFilterChain);
            }
        }

        serverWebExchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return serverWebExchange.getResponse().setComplete();
    }
}
