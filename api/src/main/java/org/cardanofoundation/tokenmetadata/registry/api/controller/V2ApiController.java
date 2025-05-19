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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

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

    private final AppConfig.CipPriorityConfiguration priorityConfiguration;

    private final Cip68FungibleTokenService cip68FungibleTokenService;

    private final V1ApiMetadataIndexer v1ApiMetadataIndexer;

    @Override
    public ResponseEntity<Response> getSubject(final String subject,
                                               final List<String> properties,
                                               final List<QueryPriority> priorities,
                                               final Boolean showCipsDetails) {

        log.info("subject: {}, properties: {}, priorities: {}, showCipsDetails: {}", subject,
                properties != null ? String.join(",", properties) : "",
                priorities != null ? priorities.stream().map(QueryPriority::name).collect(Collectors.joining(",")) : "",
                showCipsDetails);

        var queryProperties = properties != null ? properties : ALL_PROPERTIES;
        var queryPriority = priorities != null ? priorities : priorityConfiguration.getDefaultPriority();

        var tokenMetadata = queryPriority.stream()
                .reduce(IDENTITY, combineStandards(subject, queryProperties), aggregateResults());

        if (tokenMetadata.first().equals(Metadata.empty())) {
            return ResponseEntity.notFound().build();
        } else {
            var standards = tokenMetadata.second();
            var stringPriorities = queryPriority.stream().map(QueryPriority::name).toList();
            var response = new Response(new Subject(subject, tokenMetadata.first(), showCipsDetails ? standards : null), stringPriorities);
            return ResponseEntity.ok(response);
        }


    }

    @Override
    public ResponseEntity<BatchResponse> getSubjects(BatchRequest body,
                                                     List<QueryPriority> priorities,
                                                     Boolean showCipsDetails) {

        var queryProperties = body.getProperties() != null ? body.getProperties() : ALL_PROPERTIES;
        var queryPriority = priorities != null ? priorities : priorityConfiguration.getDefaultPriority();

        var subjects = body.getSubjects()
                .stream()
                .map(subject -> {
                    var pair = queryPriority.stream().reduce(IDENTITY, combineStandards(subject, queryProperties), aggregateResults());
                    return new Subject(subject, pair.first(), showCipsDetails ? pair.second() : null);
                })
                .toList();
        var stringPriorities = queryPriority.stream().map(QueryPriority::name).toList();
        return ResponseEntity.ok(new BatchResponse(subjects, stringPriorities));
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
            var metadata = thisMetadata.first().merge(that.first());
            var standards = thisMetadata.second().merge(that.second());
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
