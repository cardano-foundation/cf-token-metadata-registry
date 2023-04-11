package org.cardanofoundation.metadatatools.api.indexer;

import java.util.List;
import org.cardanofoundation.metadatatools.api.model.rest.WalletFraudIncident;

public interface ExtendedMetadataIndexer {
  List<WalletFraudIncident> findScamIncidents(final List<String> walletAddressHashes);
}
