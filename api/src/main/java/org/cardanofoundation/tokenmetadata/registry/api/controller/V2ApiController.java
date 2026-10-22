package org.cardanofoundation.tokenmetadata.registry.api.controller;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.api.config.AppConfig;
import org.cardanofoundation.tokenmetadata.registry.api.indexer.V1ApiMetadataIndexer;
import org.cardanofoundation.tokenmetadata.registry.api.model.BatchPrefetchData;
import org.cardanofoundation.tokenmetadata.registry.api.model.QueryPriority;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip113.ProgrammableTokenCip113;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.BatchRequest;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip68.FungibleTokenMetadata;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.TokenMetadata;
import org.cardanofoundation.tokenmetadata.registry.api.model.v2.BatchResponse;
import org.cardanofoundation.tokenmetadata.registry.api.model.v2.Extension;
import org.cardanofoundation.tokenmetadata.registry.api.model.v2.Metadata;
import org.cardanofoundation.tokenmetadata.registry.api.model.v2.Response;
import org.cardanofoundation.tokenmetadata.registry.api.model.v2.Standards;
import org.cardanofoundation.tokenmetadata.registry.api.model.v2.Subject;
import org.cardanofoundation.tokenmetadata.registry.api.model.v2.TokenType;
import org.cardanofoundation.tokenmetadata.registry.api.model.v2.ValidationScope;
import org.cardanofoundation.tokenmetadata.registry.entity.MetadataReferenceNft;
import org.cardanofoundation.tokenmetadata.registry.api.service.Cip68FungibleTokenService;
import org.cardanofoundation.tokenmetadata.registry.api.service.Cip68UserTokenService;
import org.cardanofoundation.tokenmetadata.registry.api.service.RegistryMetricsService;
import org.cardanofoundation.tokenmetadata.registry.api.service.cip113.Cip113RegistryService;
import org.cardanofoundation.tokenmetadata.registry.api.util.AssetType;
import org.cardanofoundation.tokenmetadata.registry.api.util.LogSanitizer;
import org.cardanofoundation.tokenmetadata.registry.validation.ValidationError;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

    private final AppConfig.CipPriorityConfiguration priorityConfiguration;
    private final Cip68FungibleTokenService cip68FungibleTokenService;
    private final Cip68UserTokenService cip68UserTokenService;
    private final V1ApiMetadataIndexer v1ApiMetadataIndexer;
    private final Cip113RegistryService cip113RegistryService;
    private final RegistryMetricsService metricsService;

    /**
     * A subject as indexed, with its validation errors (ADR-019). It is a 404 only when no standard has anything indexed
     * for it, or when {@code validation} does not accept it; a token with errors, even without a name or a description,
     * is a 200 under the default {@code ALL}.
     */
    @Override
    public ResponseEntity<Response> getSubject(final String subject,
                                               final List<String> properties,
                                               final List<QueryPriority> priorities,
                                               final Boolean showCipsDetails,
                                               final ValidationScope validation) {

        log.info("subject: {}, properties: {}, priorities: {}, showCipsDetails: {}, validation: {}",
                LogSanitizer.sanitizeHex(subject),
                properties != null ? properties.stream().map(LogSanitizer::sanitize).collect(Collectors.joining(",")) : "",
                priorities != null ? priorities.stream().map(QueryPriority::name).collect(Collectors.joining(",")) : "",
                showCipsDetails, validation);

        metricsService.recordV2Query(1);

        List<String> queryProperties = properties != null ? properties : ALL_PROPERTIES;
        List<QueryPriority> queryPriority = priorities != null ? priorities : priorityConfiguration.getDefaultPriority();
        ValidationScope scope = validation != null ? validation : ValidationScope.ALL;

        ResolvedMetadata resolved = queryPriority.stream()
                .reduce(IDENTITY, combineStandards(subject, queryProperties), aggregateResults());
        if (resolved.standards().isEmpty()) {
            metricsService.recordNotFound();
            return ResponseEntity.notFound().build();
        }

        Map<String, Extension> extensions = buildExtensions(subject);
        Subject resolvedSubject = toSubject(subject, resolved, extensions, Boolean.TRUE.equals(showCipsDetails));
        if (!scope.accepts(resolvedSubject)) {
            metricsService.recordNotFound();
            return ResponseEntity.notFound().build();
        }

        recordCipHits(resolved.standards());
        if (!extensions.isEmpty()) {
            metricsService.recordCip113Hit();
        }
        List<String> stringPriorities = queryPriority.stream().map(QueryPriority::name).toList();
        return ResponseEntity.ok(new Response(resolvedSubject, stringPriorities));
    }

    @Override
    public ResponseEntity<BatchResponse> getSubjects(BatchRequest body,
                                                     List<QueryPriority> priorities,
                                                     Boolean showCipsDetails,
                                                     ValidationScope validation) {
        metricsService.recordV2Query(body.getSubjects().size());

        List<String> queryProperties = body.getProperties() != null ? body.getProperties() : ALL_PROPERTIES;
        List<QueryPriority> queryPriority = priorities != null ? priorities : priorityConfiguration.getDefaultPriority();
        ValidationScope scope = validation != null ? validation : ValidationScope.ALL;

        // Pre-fetch all data to avoid N+1 queries (4 bulk queries upfront)
        BatchPrefetchData prefetch = prefetchBatch(body.getSubjects(), queryProperties);
        boolean includeCipsDetails = Boolean.TRUE.equals(showCipsDetails);

        List<Subject> subjects = body.getSubjects()
                .stream()
                .map(subject -> buildSubject(subject, queryPriority, queryProperties, prefetch, includeCipsDetails, scope))
                .flatMap(Optional::stream)
                .toList();
        List<String> stringPriorities = queryPriority.stream().map(QueryPriority::name).toList();

        return ResponseEntity.ok(new BatchResponse(subjects, stringPriorities));
    }

    /**
     * The subject of the response. {@code metadata.valid} says whether the token follows its CIP completely: no
     * validation errors. A token indexed before validation existed has none recorded ({@code null}, not empty) and falls
     * back to the rule {@code valid} always had: name and description present.
     */
    private static Subject toSubject(String subject, ResolvedMetadata resolved, Map<String, Extension> extensions,
                                     boolean includeCipsDetails) {
        List<ValidationError> errors = resolved.validationErrors();
        boolean valid = errors != null
                ? errors.isEmpty()
                : resolved.metadata().name() != null && resolved.metadata().description() != null;
        Metadata metadata = resolved.metadata().toBuilder().valid(valid).build();
        TokenType type = extensions.isEmpty() ? TokenType.NATIVE : TokenType.PROGRAMMABLE;
        return new Subject(subject, type, metadata, includeCipsDetails ? resolved.standards() : null,
                extensions.isEmpty() ? null : extensions, errors);
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
                    .map(metadata -> cip26(metadata, v1ApiMetadataIndexer.findValidationErrors(List.of(subject)).get(subject),
                            properties));
            // a 333 or 444 subject is served from CIP-68 only when its user token was seen on chain (ADR-022)
            case CIP_68 -> cip68FungibleTokenService.getReferenceNftSubject(subject)
                    .filter(referenceNft -> cip68UserTokenService.exists(subject))
                    .flatMap(assetType -> cip68FungibleTokenService.findSubject(assetType.policyId(), assetType.assetName(), properties,
                            cip68FungibleTokenService.isRichFungibleToken(subject)))
                    .map(fungibleTokenMetadata -> cip68(fungibleTokenMetadata, properties));
        };
    }

    /** CIP-26 metadata with its errors, limited to the requested properties; {@code standards.cip26} carries them too. */
    private static ResolvedMetadata cip26(TokenMetadata metadata, @Nullable List<ValidationError> errors, List<String> properties) {
        List<ValidationError> scoped = PropertyErrorFilter.filter(errors, properties);
        metadata.setValidationErrors(scoped);
        return new ResolvedMetadata(Metadata.from(metadata), new Standards(metadata, null), scoped);
    }

    /** CIP-68 metadata with the errors of its label, limited to the requested properties. */
    private static ResolvedMetadata cip68(FungibleTokenMetadata fungibleTokenMetadata, List<String> properties) {
        FungibleTokenMetadata scoped = fungibleTokenMetadata.withValidationErrors(
                PropertyErrorFilter.filter(fungibleTokenMetadata.validationErrors(), properties));
        return new ResolvedMetadata(Metadata.from(scoped), new Standards(null, scoped), scoped.validationErrors());
    }

    private BatchPrefetchData prefetchBatch(List<String> subjects, List<String> queryProperties) {
        List<String> policyIds = subjects.stream()
                .map(s -> AssetType.fromUnit(s).policyId())
                .distinct()
                .toList();
        Map<String, TokenMetadata> cip26Map = v1ApiMetadataIndexer.findSubjectsSelectProperties(subjects, queryProperties);
        Map<String, List<ValidationError>> cip26ErrorsMap = cip26Map.isEmpty()
                ? Map.of()
                : v1ApiMetadataIndexer.findValidationErrors(cip26Map.keySet());
        Map<String, MetadataReferenceNft> cip68Map = cip68FungibleTokenService.findLatestByPolicyIds(policyIds);
        Map<String, ProgrammableTokenCip113> cip113Map = cip113RegistryService.findByPolicyIds(policyIds);
        Set<String> cip68UserTokens = cip68UserTokenService.existing(subjects);

        return new BatchPrefetchData(cip26Map, cip26ErrorsMap, cip68Map, cip113Map, cip68UserTokens);
    }

    private Optional<ResolvedMetadata> findMetadataBatch(String subject,
                                                         List<String> properties,
                                                         QueryPriority priority,
                                                         BatchPrefetchData prefetch) {
        return switch (priority) {
            case CIP_26 -> Optional.ofNullable(prefetch.cip26Map().get(subject))
                    .map(metadata -> cip26(metadata, prefetch.cip26ErrorsMap().get(subject), properties));

            case CIP_68 -> cip68FungibleTokenService.getReferenceNftSubject(subject)
                    .filter(referenceNft -> prefetch.cip68UserTokens().contains(Cip68UserTokenService.normalize(subject)))
                    .flatMap(assetType -> cip68FungibleTokenService.findSubject(assetType.policyId(), assetType.assetName(), properties, prefetch.cip68Map(),
                            cip68FungibleTokenService.isRichFungibleToken(subject)))
                    .map(fungibleTokenMetadata -> cip68(fungibleTokenMetadata, properties));
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
                .ifPresent(cip113 -> extensions.put(ProgrammableTokenCip113.EXTENSION_KEY, cip113));
        return extensions;
    }

    /** The subject of a batch, or empty when nothing is indexed for it or {@code scope} does not accept it. */
    private Optional<Subject> buildSubject(String subject,
                                           List<QueryPriority> queryPriority,
                                           List<String> queryProperties,
                                           BatchPrefetchData prefetch,
                                           boolean includeCipsDetails,
                                           ValidationScope scope) {
        ResolvedMetadata resolved = queryPriority.stream()
                .reduce(IDENTITY, combineStandardsBatch(subject, queryProperties, prefetch), aggregateResults());
        if (resolved.standards().isEmpty()) {
            metricsService.recordNotFound();
            return Optional.empty();
        }

        Map<String, Extension> extensions = new LinkedHashMap<>();
        ProgrammableTokenCip113 cip113 = prefetch.cip113Map().get(AssetType.fromUnit(subject).policyId());
        if (cip113 != null) {
            extensions.put(ProgrammableTokenCip113.EXTENSION_KEY, cip113);
        }
        Subject resolvedSubject = toSubject(subject, resolved, extensions, includeCipsDetails);
        if (!scope.accepts(resolvedSubject)) {
            return Optional.empty();
        }
        recordCipHits(resolved.standards());
        if (cip113 != null) {
            metricsService.recordCip113Hit();
        }
        return Optional.of(resolvedSubject);
    }

    /**
     * The metadata assembled from the standards in query priority order: the first standard wins per property, and the
     * validation errors of every standard are kept, in that order.
     */
    private record ResolvedMetadata(Metadata metadata, Standards standards, @Nullable List<ValidationError> validationErrors) {

        static ResolvedMetadata empty() {
            return new ResolvedMetadata(Metadata.empty(), Standards.empty(), List.of());
        }

        ResolvedMetadata merge(ResolvedMetadata that) {
            List<ValidationError> errors = new ArrayList<>();
            if (validationErrors != null) {
                errors.addAll(validationErrors);
            }
            if (that.validationErrors() != null) {
                errors.addAll(that.validationErrors());
            }
            // unchecked (null) when a standard has no errors recorded and nothing else reports any: "no errors" would be a claim
            boolean unchecked = validationErrors == null || that.validationErrors() == null;
            return new ResolvedMetadata(metadata.merge(that.metadata()), standards.merge(that.standards()),
                    unchecked && errors.isEmpty() ? null : errors);
        }
    }

}
