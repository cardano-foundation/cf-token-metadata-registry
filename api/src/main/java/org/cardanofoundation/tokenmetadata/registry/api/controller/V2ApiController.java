package org.cardanofoundation.tokenmetadata.registry.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.api.config.AppConfig;
import org.cardanofoundation.tokenmetadata.registry.api.indexer.V1ApiMetadataIndexer;
import org.cardanofoundation.tokenmetadata.registry.api.model.BatchPrefetchData;
import org.cardanofoundation.tokenmetadata.registry.api.model.QueryPriority;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip113.ProgrammableTokenCip113;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.BatchRequest;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.TokenMetadata;
import org.cardanofoundation.tokenmetadata.registry.entity.MetadataReferenceNft;
import org.cardanofoundation.tokenmetadata.registry.api.model.v2.Extension;
import org.cardanofoundation.tokenmetadata.registry.api.model.v2.Metadata;
import org.cardanofoundation.tokenmetadata.registry.api.model.v2.Response;
import org.cardanofoundation.tokenmetadata.registry.api.model.v2.BatchResponse;
import org.cardanofoundation.tokenmetadata.registry.api.model.v2.Standards;
import org.cardanofoundation.tokenmetadata.registry.api.model.v2.Subject;
import org.cardanofoundation.tokenmetadata.registry.api.model.v2.TokenType;
import org.cardanofoundation.tokenmetadata.registry.api.service.Cip68FungibleTokenService;
import org.cardanofoundation.tokenmetadata.registry.api.service.RegistryMetricsService;
import org.cardanofoundation.tokenmetadata.registry.api.service.cip113.Cip113RegistryService;
import org.cardanofoundation.tokenmetadata.registry.api.util.AssetType;
import org.cardanofoundation.tokenmetadata.registry.api.util.LogSanitizer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/v2")
@Slf4j
public class V2ApiController implements V2Api {

    private static final ResolvedMetadata IDENTITY = ResolvedMetadata.empty();

    private static final List<String> ALL_PROPERTIES = List.of();

    private static final List<String> REQUIRED_PROPERTIES = List.of("name", "description");

    private final AppConfig.CipPriorityConfiguration priorityConfiguration;
    private final Cip68FungibleTokenService cip68FungibleTokenService;
    private final V1ApiMetadataIndexer v1ApiMetadataIndexer;
    private final Cip113RegistryService cip113RegistryService;
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

        ResolvedMetadata tokenMetadata = queryPriority.stream()
                .reduce(IDENTITY, combineStandards(subject, queryProperties), aggregateResults());

        if (tokenMetadata.metadata().isEmpty() || !tokenMetadata.metadata().isValid()) {
            metricsService.recordNotFound();

            return ResponseEntity.notFound().build();
        }

        recordCipHits(tokenMetadata.standards());
        Standards standards = tokenMetadata.standards();
        Map<String, Extension> extensions = buildExtensions(subject);
        TokenType type = extensions.isEmpty() ? TokenType.NATIVE : TokenType.PROGRAMMABLE;
        boolean includeCipsDetails = Boolean.TRUE.equals(showCipsDetails);
        List<String> stringPriorities = queryPriority.stream().map(QueryPriority::name).toList();
        Response response = new Response(new Subject(subject, type, tokenMetadata.metadata(),
                includeCipsDetails ? standards : null, extensions), stringPriorities);

        return ResponseEntity.ok(response);
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

        // Pre-fetch all data to avoid N+1 queries (3 bulk queries upfront)
        BatchPrefetchData prefetch = prefetchBatch(body.getSubjects(), queryProperties);
        boolean includeCipsDetails = Boolean.TRUE.equals(showCipsDetails);

        List<Subject> subjects = body.getSubjects()
                .stream()
                .map(subject -> buildSubject(subject, queryPriority, queryProperties, prefetch, includeCipsDetails))
                .filter(s -> !s.metadata().isEmpty() && s.metadata().isValid())
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

    private Optional<ResolvedMetadata> findMetadata(String subject, List<String> properties, QueryPriority priority) {
        return switch (priority) {
            case CIP_26 -> v1ApiMetadataIndexer.findSubjectSelectProperties(subject, properties)
                    .map(metadata -> new ResolvedMetadata(Metadata.from(metadata), new Standards(metadata, null)));
            case CIP_68 -> cip68FungibleTokenService.getReferenceNftSubject(subject)
                    .flatMap(assetType -> cip68FungibleTokenService.findSubject(assetType.policyId(), assetType.assetName(), properties))
                    .map(fungibleTokenMetadata -> new ResolvedMetadata(Metadata.from(fungibleTokenMetadata), new Standards(null, fungibleTokenMetadata)));
        };
    }

    private BatchPrefetchData prefetchBatch(List<String> subjects, List<String> queryProperties) {
        List<String> policyIds = subjects.stream()
                .map(s -> AssetType.fromUnit(s).policyId())
                .distinct()
                .toList();
        Map<String, TokenMetadata> cip26Map = v1ApiMetadataIndexer.findSubjectsSelectProperties(subjects, queryProperties);
        Map<String, MetadataReferenceNft> cip68Map = cip68FungibleTokenService.findLatestByPolicyIds(policyIds);
        Map<String, ProgrammableTokenCip113> cip113Map = cip113RegistryService.findByPolicyIds(policyIds);

        return new BatchPrefetchData(cip26Map, cip68Map, cip113Map);
    }

    private Optional<ResolvedMetadata> findMetadataBatch(String subject,
                                                         List<String> properties,
                                                         QueryPriority priority,
                                                         BatchPrefetchData prefetch) {
        return switch (priority) {
            case CIP_26 -> Optional.ofNullable(prefetch.cip26Map().get(subject))
                    .map(metadata -> new ResolvedMetadata(Metadata.from(metadata), new Standards(metadata, null)));

            case CIP_68 -> cip68FungibleTokenService.getReferenceNftSubject(subject)
                    .flatMap(assetType -> cip68FungibleTokenService.findSubject(assetType.policyId(), assetType.assetName(), properties, prefetch.cip68Map()))
                    .map(fungibleTokenMetadata -> new ResolvedMetadata(Metadata.from(fungibleTokenMetadata), new Standards(null, fungibleTokenMetadata)));
        };
    }

    private static BinaryOperator<ResolvedMetadata> aggregateResults() {
        return ResolvedMetadata::merge;
    }

    private BiFunction<ResolvedMetadata, QueryPriority, ResolvedMetadata> combineStandards(String subject, List<String> properties) {
        return (accumulatedResults, priority) -> findMetadata(subject, properties, priority)
                .map(accumulatedResults::merge)
                .orElse(accumulatedResults);
    }

    private BiFunction<ResolvedMetadata, QueryPriority, ResolvedMetadata> combineStandardsBatch(
            String subject, List<String> properties, BatchPrefetchData prefetch) {
        return (accumulatedResults, priority) -> findMetadataBatch(subject, properties, priority, prefetch)
                .map(accumulatedResults::merge)
                .orElse(accumulatedResults);
    }

    private Map<String, Extension> buildExtensions(String subject) {
        Map<String, Extension> extensions = new LinkedHashMap<>();
        cip113RegistryService.findByPolicyId(AssetType.fromUnit(subject).policyId())
                .ifPresent(cip113 -> {
                    extensions.put(ProgrammableTokenCip113.EXTENSION_KEY, cip113);
                    metricsService.recordCip113Hit();
                });

        return extensions;
    }

    private Subject buildSubject(String subject,
                                 List<QueryPriority> queryPriority,
                                 List<String> queryProperties,
                                 BatchPrefetchData prefetch,
                                 boolean includeCipsDetails) {
        ResolvedMetadata resolved = queryPriority.stream()
                .reduce(IDENTITY, combineStandardsBatch(subject, queryProperties, prefetch), aggregateResults());

        Map<String, Extension> extensions = new LinkedHashMap<>();
        ProgrammableTokenCip113 cip113 = prefetch.cip113Map().get(AssetType.fromUnit(subject).policyId());
        if (cip113 != null) {
            extensions.put(ProgrammableTokenCip113.EXTENSION_KEY, cip113);
            metricsService.recordCip113Hit();
        }

        if (resolved.metadata().isEmpty()) {
            metricsService.recordNotFound();
        } else {
            recordCipHits(resolved.standards());
        }

        TokenType type = extensions.isEmpty() ? TokenType.NATIVE : TokenType.PROGRAMMABLE;

        return new Subject(subject, type, resolved.metadata(),
                includeCipsDetails ? resolved.standards() : null,
                extensions.isEmpty() ? null : extensions);
    }

    private record ResolvedMetadata(Metadata metadata, Standards standards) {

        static ResolvedMetadata empty() {
            return new ResolvedMetadata(Metadata.empty(), Standards.empty());
        }

        ResolvedMetadata merge(ResolvedMetadata that) {
            return new ResolvedMetadata(metadata.merge(that.metadata()), standards.merge(that.standards()));
        }

    }

}
