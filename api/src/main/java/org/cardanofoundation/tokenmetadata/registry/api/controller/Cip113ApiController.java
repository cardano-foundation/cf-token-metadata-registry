package org.cardanofoundation.tokenmetadata.registry.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip113.Cip113BatchRequest;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip113.Cip113RegistryEntry;
import org.cardanofoundation.tokenmetadata.registry.api.service.RegistryMetricsService;
import org.cardanofoundation.tokenmetadata.registry.api.service.cip113.Cip113RegistryService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/v2/cip113")
@Slf4j
public class Cip113ApiController implements Cip113Api {

    private final Cip113RegistryService cip113RegistryService;
    private final RegistryMetricsService metricsService;

    @Override
    public ResponseEntity<Cip113RegistryEntry> getRegistryEntry(String policyId) {
        return cip113RegistryService.findRegistryEntry(policyId)
                .map(entry -> {
                    metricsService.recordCip113Hit();
                    return ResponseEntity.ok(entry);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<List<Cip113RegistryEntry>> queryRegistry(Cip113BatchRequest body) {
        List<Cip113RegistryEntry> entries = cip113RegistryService.findRegistryEntries(body.policyIds());
        return ResponseEntity.ok(entries);
    }

}
