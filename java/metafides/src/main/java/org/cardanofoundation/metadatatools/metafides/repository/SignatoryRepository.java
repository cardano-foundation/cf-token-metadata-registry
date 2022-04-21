package org.cardanofoundation.metadatatools.metafides.repository;

import org.cardanofoundation.metadatatools.metafides.model.data.Signatory;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface SignatoryRepository  extends ReactiveCrudRepository<Signatory, String> {
}
