package org.cardanofoundation.tokenmetadata.registry.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.api.config.AppConfig;
import org.cardanofoundation.tokenmetadata.registry.api.indexer.V1ApiMetadataIndexer;
import org.cardanofoundation.tokenmetadata.registry.api.model.Pair;
import org.cardanofoundation.tokenmetadata.registry.api.model.QueryPriority;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.BatchRequest;
import org.cardanofoundation.tokenmetadata.registry.api.model.v2.*;
import org.cardanofoundation.tokenmetadata.registry.api.service.Cip68FungibleTokenService;
import org.cardanofoundation.tokenmetadata.registry.api.service.RegistryMetricsService;
import org.cardanofoundation.tokenmetadata.registry.api.util.LogSanitizer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/v2")
@Slf4j
public class V2ApiController implements V2Api {

    public static final Pair<Metadata, Standards> IDENTITY = new Pair<>(Metadata.empty(), Standards.empty());

    private static final List<String> ALL_PROPERTIES = List.of();

    private static final List<String> REQUIRED_PROPERTIES = List.of("name", "description");

    private final AppConfig.CipPriorityConfiguration priorityConfiguration;

    private final Cip68FungibleTokenService cip68FungibleTokenService;

    private final V1ApiMetadataIndexer v1ApiMetadataIndexer;

    private final RegistryMetricsService metricsService;

    @Override
    public ResponseEntity<Response> getSubject(final String subject,
                                               final List<String> properties,
                                               final List<QueryPriority> priorities,
                                               final Boolean showCipsDetails) {

        log.info("subject: {}, properties: {}, priorities: {}, showCipsDetails: {}",
                LogSanitizer.sanitizeHex(subject),
                properties != null ? String.join(",", properties) : "",
                priorities != null ? priorities.stream().map(QueryPriority::name).collect(Collectors.joining(",")) : "",
                showCipsDetails);

        metricsService.recordV2Query(1);

        List<String> queryProperties = properties != null ? properties : ALL_PROPERTIES;
        if (!queryProperties.isEmpty() && !queryProperties.containsAll(REQUIRED_PROPERTIES)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "When filtering properties, 'name' and 'description' are required and must be included");
        }
        List<QueryPriority> queryPriority = priorities != null ? priorities : priorityConfiguration.getDefaultPriority();

        Pair<Metadata, Standards> tokenMetadata = queryPriority.stream()
                .reduce(IDENTITY, combineStandards(subject, queryProperties), aggregateResults());

        if (tokenMetadata.first().isEmpty() || !tokenMetadata.first().isValid()) {
            metricsService.recordNotFound();
            return ResponseEntity.notFound().build();
        } else {
            recordCipHits(tokenMetadata.second());
            Standards standards = tokenMetadata.second();
            List<String> stringPriorities = queryPriority.stream().map(QueryPriority::name).toList();
            Response response = new Response(new Subject(subject, tokenMetadata.first(), showCipsDetails ? standards : null), stringPriorities);

            return ResponseEntity.ok(response);
        }
    }

    @Override
    public ResponseEntity<BatchResponse> getSubjects(BatchRequest body,
                                                     List<QueryPriority> priorities,
                                                     Boolean showCipsDetails) {
        metricsService.recordV2Query(body.getSubjects().size());

        List<String> queryProperties = body.getProperties() != null ? body.getProperties() : ALL_PROPERTIES;
        if (!queryProperties.isEmpty() && !queryProperties.containsAll(REQUIRED_PROPERTIES)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "When filtering properties, 'name' and 'description' are required and must be included");
        }
        List<QueryPriority> queryPriority = priorities != null ? priorities : priorityConfiguration.getDefaultPriority();

        List<Subject> subjects = body.getSubjects()
                .stream()
                .map(subject -> {
                    Pair<Metadata, Standards> pair = queryPriority.stream().reduce(IDENTITY, combineStandards(subject, queryProperties), aggregateResults());
                    Subject subjectResult = new Subject(subject, pair.first(), showCipsDetails ? pair.second() : null);
                    if (pair.first().isEmpty()) {
                        metricsService.recordNotFound();
                    } else {
                        recordCipHits(pair.second());
                    }
                    return subjectResult;
                })
                .filter(metadata -> !metadata.metadata().isEmpty() && metadata.metadata().isValid())
                .toList();
        List<String> stringPriorities = queryPriority.stream().map(QueryPriority::name).toList();
        return ResponseEntity.ok(new BatchResponse(subjects, stringPriorities));
    }


    private void recordCipHits(Standards standards) {
        if (standards.cip26() != null) {
            metricsService.recordCip26Hit();
        }
        if (standards.cip68() != null) {
            metricsService.recordCip68Hit();
        }
    }

    private Optional<Pair<Metadata, Standards>> findMetadata(String subject, List<String> properties, QueryPriority priority) {
        return switch (priority) {
            case CIP_26 -> v1ApiMetadataIndexer.findSubjectSelectProperties(subject, properties)
                    .map(metadata -> new Pair<>(Metadata.from(metadata), new Standards(metadata, null)));
            case CIP_68 -> cip68FungibleTokenService.getReferenceNftSubject(subject)
                    .flatMap(assetType -> cip68FungibleTokenService.findSubject(assetType.policyId(), assetType.assetName(), properties))
                    .map(fungibleTokenMetadata -> new Pair<>(Metadata.from(fungibleTokenMetadata), new Standards(null, fungibleTokenMetadata)));
        };
    }

    private static BinaryOperator<Pair<Metadata, Standards>> aggregateResults() {
        return (thisMetadata, that) -> {
            Metadata metadata = thisMetadata.first().merge(that.first());
            Standards standards = thisMetadata.second().merge(that.second());
            return new Pair<>(metadata, standards);
        };
    }

    private BiFunction<Pair<Metadata, Standards>, QueryPriority, Pair<Metadata, Standards>> combineStandards(String subject, List<String> properties) {
        return (accumulatedResults, priority) -> findMetadata(subject, properties, priority)
                .map(metadataObjectPair -> new Pair<>(accumulatedResults.first().merge(metadataObjectPair.first()),
                        accumulatedResults.second().merge(metadataObjectPair.second())))
                .orElse(accumulatedResults);
    }

}
