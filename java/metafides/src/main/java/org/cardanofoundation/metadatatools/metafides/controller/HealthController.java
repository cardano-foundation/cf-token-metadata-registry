package org.cardanofoundation.metadatatools.metafides.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/health")
public class HealthController {
    @RequestMapping(value = "", method = RequestMethod.GET, produces = "application/json")
    public Mono<ResponseEntity<?>> getHealthStatus() {
        return Mono.just(ResponseEntity.ok().build());
    }
}
