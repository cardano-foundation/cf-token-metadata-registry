package org.cardanofoundation.metadatatools.metafides.controller;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.cardanofoundation.metadatatools.metafides.config.OAuth2Config;
import org.cardanofoundation.metadatatools.metafides.model.AccessTokenResponse;
import org.cardanofoundation.metadatatools.metafides.model.OAuth2GrantType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/oauth2")
public class OAuth2Controller {
    @Autowired
    OAuth2Config cognitoConfig;

    private WebClient getWebClient() {
        final HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, cognitoConfig.getRequestTimeout())
                .responseTimeout(Duration.ofMillis(cognitoConfig.getRequestTimeout()))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(cognitoConfig.getRequestTimeout(), TimeUnit.MILLISECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(cognitoConfig.getRequestTimeout(), TimeUnit.MILLISECONDS)));

        return WebClient.builder()
                .baseUrl(cognitoConfig.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @RequestMapping(value = "/token", method = RequestMethod.GET, produces = "application/json")
    public Mono<AccessTokenResponse> getToken(@RequestAttribute("authHeader") final String authHeader, @RequestParam("grant_type") final OAuth2GrantType grantType) {
        final WebClient webClient = getWebClient();
        final MultiValueMap<String, String> oauth2Params = new LinkedMultiValueMap<>();
        oauth2Params.add("grant_type", grantType.getValue());
        return webClient
                .post()
                .uri("/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(oauth2Params))
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .retrieve().bodyToMono(AccessTokenResponse.class);
    }
}
