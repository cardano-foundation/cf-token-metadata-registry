package org.cardanofoundation.tokenmetadata.registry.api.indexer;

import org.cardanofoundation.tokenmetadata.registry.validation.ValidationError;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.TokenMetadata;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface V1ApiMetadataIndexer {

    Map<String, TokenMetadata> findSubjectsSelectProperties(final List<String> subjects, final List<String> properties);

    Optional<TokenMetadata> findSubject(final String subject);

    Optional<TokenMetadata> findSubjectSelectProperties(final String subject, final List<String> properties);

    /**
     * The CIP-26 validation errors of the given subjects (ADR-019), for the V2 API: the V1 API serves the entries in the
     * registry server's own format, without them. A subject stored before validation existed maps to {@code null} (never
     * checked), which is not the same as an empty list; a subject that is not stored is not in the map.
     */
    Map<String, List<ValidationError>> findValidationErrors(final Collection<String> subjects);

}
