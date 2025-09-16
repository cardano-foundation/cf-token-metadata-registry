package org.cardanofoundation.tokenmetadata.registry.api.indexer;

import org.cardanofoundation.tokenmetadata.registry.api.model.rest.TokenMetadata;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface V1ApiMetadataIndexer {

    Map<String, TokenMetadata> findSubjectsSelectProperties(final List<String> subjects, final List<String> properties);

    Optional<TokenMetadata> findSubject(final String subject);

    Optional<TokenMetadata> findSubjectSelectProperties(final String subject, final List<String> properties);

}
