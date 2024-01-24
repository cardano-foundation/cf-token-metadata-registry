package org.cardanofoundation.tokenmetadata.registry.api.indexer;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.TokenMetadata;

public interface V1ApiMetadataIndexer {
  Map<String, TokenMetadata> findSubjectsSelectProperties(
      final String metadataSourceName, final List<String> subjects, final List<String> properties);

  Optional<TokenMetadata> findSubject(final String metadataSourceName, final String subject);

  Optional<TokenMetadata> findSubjectSelectProperties(
      final String metadataSourceName, final String subject, final List<String> properties);
}
