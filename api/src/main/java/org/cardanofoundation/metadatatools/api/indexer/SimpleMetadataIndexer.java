package org.cardanofoundation.metadatatools.api.indexer;

import org.cardanofoundation.metadatatools.api.model.rest.TokenMetadata;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface SimpleMetadataIndexer {
    Map<String, TokenMetadata> findSubjectsSelectProperties(final String metadataSourceName, final List<String> subjects, final List<String> properties);

    Optional<TokenMetadata> findSubject(final String metadataSourceName, final String subject);

    Optional<TokenMetadata> findSubjectSelectProperties(final String metadataSourceName, final String subject, final List<String> properties);
}
