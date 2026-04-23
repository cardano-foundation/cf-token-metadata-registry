package org.cardanofoundation.tokenmetadata.registry.api.controller;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.AssetsExtStoreProperties;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.QueryPriority;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.Subject;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.SubjectBatchResponse;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.SubjectResponse;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.service.TokenQueryService;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.service.TokenQueryService.BatchPrefetchData;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.BatchRequest;
import org.cardanofoundation.tokenmetadata.registry.api.service.RegistryMetricsService;
import org.cardanofoundation.tokenmetadata.registry.api.util.LogSanitizer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/v2")
@Slf4j
public class V2ApiController implements V2Api {

    private static final List<String> ALL_PROPERTIES = List.of();
    private static final List<String> REQUIRED_PROPERTIES = List.of("name", "description");

    private final TokenQueryService tokenQueryService;
    private final AssetsExtStoreProperties assetsStoreProperties;
    private final RegistryMetricsService metricsService;

    private List<QueryPriority> defaultQueryPriority;

    @PostConstruct
    void init() {
        defaultQueryPriority = Arrays.stream(assetsStoreProperties.getDefaultQueryPriority().split(","))
                .map(String::trim)
                .map(QueryPriority::valueOf)
                .toList();
    }

    @Override
    public ResponseEntity<SubjectResponse> getSubject(final String subject,
                                                      final List<String> properties,
                                                      final List<QueryPriority> priorities,
                                                      final Boolean showCipsDetails) {

        if (log.isDebugEnabled()) {
            log.debug("subject: {}, properties: {}, priorities: {}, showCipsDetails: {}",
                    LogSanitizer.sanitizeHex(subject),
                    properties != null ? String.join(",", properties) : "",
                    priorities != null ? priorities.stream().map(QueryPriority::name).collect(Collectors.joining(",")) : "",
                    showCipsDetails);
        }

        List<String> queryProperties = properties != null ? properties : ALL_PROPERTIES;
        validateProperties(queryProperties);
        List<QueryPriority> queryPriority = priorities != null ? priorities : defaultQueryPriority;
        boolean includeCipsDetails = Boolean.TRUE.equals(showCipsDetails);

        List<String> stringPriorities = queryPriority.stream().map(QueryPriority::name).toList();
        return tokenQueryService.querySubject(subject, queryPriority, queryProperties, includeCipsDetails)
                .map(result -> {
                    metricsService.recordSubject(result);
                    return ResponseEntity.ok(new SubjectResponse(result, stringPriorities));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<SubjectBatchResponse> getSubjects(final BatchRequest body,
                                                            final List<QueryPriority> priorities,
                                                            final Boolean showCipsDetails) {
        List<String> queryProperties = body.getProperties() != null ? body.getProperties() : ALL_PROPERTIES;
        validateProperties(queryProperties);
        List<QueryPriority> queryPriority = priorities != null ? priorities : defaultQueryPriority;
        List<String> stringPriorities = queryPriority.stream().map(QueryPriority::name).toList();

        if (body.getSubjects().isEmpty()) {
            return ResponseEntity.ok(new SubjectBatchResponse(List.of(), stringPriorities));
        }

        boolean includeCipsDetails = Boolean.TRUE.equals(showCipsDetails);
        BatchPrefetchData prefetch = tokenQueryService.prefetchBatch(body.getSubjects(), queryProperties);

        List<Subject> subjects = body.getSubjects()
                .stream()
                .map(subject -> tokenQueryService.querySubjectBatch(
                        subject, queryPriority, queryProperties, prefetch, includeCipsDetails))
                .filter(s -> !s.metadata().isEmpty() && s.metadata().isValid())
                .toList();
        subjects.forEach(metricsService::recordSubject);

        return ResponseEntity.ok(new SubjectBatchResponse(subjects, stringPriorities));
    }

    private void validateProperties(List<String> queryProperties) {
        if (!queryProperties.isEmpty() && !queryProperties.containsAll(REQUIRED_PROPERTIES)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "When filtering properties, 'name' and 'description' are required and must be included");
        }
    }

}
