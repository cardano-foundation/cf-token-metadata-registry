package org.cardanofoundation.metadatatools.api.indexer;

import java.time.LocalDate;
import java.util.List;
import org.cardanofoundation.metadatatools.api.model.rest.*;

public interface V2ApiMetadataIndexer {
  List<WalletFraudIncident> findScamIncidents(final List<String> walletAddressHashes);

  FetchMetadataResultSet fetchMetadata(
      final String fields,
      final String sortBy,
      final String name,
      final FilterOperand nameOp,
      final String ticker,
      final FilterOperand tickerOp,
      final String description,
      final FilterOperand descriptionOp,
      final String url,
      final FilterOperand urlOp,
      final String policy,
      final FilterOperand policyOp,
      final LocalDate updated,
      final FilterOperand updatedOp,
      final String updatedBy,
      final FilterOperand updatedbyOp,
      final Integer decimals,
      final FilterOperand decimalsOp,
      final String q,
      final String vkey,
      final Integer pageSize,
      final Long page,
      final String pivotId,
      final PivotDirection pivotDirection,
      final String metadataSourceName);
}
