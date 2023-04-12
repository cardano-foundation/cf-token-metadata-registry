package org.cardanofoundation.metadatatools.api.indexer;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.cardanofoundation.metadatatools.api.model.rest.TokenMetadata;

@Data
@Builder
public class FetchMetadataResultSet {
  private List<TokenMetadata> results;
  private long totalCount;
}
