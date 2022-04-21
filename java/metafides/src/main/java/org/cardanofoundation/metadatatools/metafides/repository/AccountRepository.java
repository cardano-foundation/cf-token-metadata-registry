package org.cardanofoundation.metadatatools.metafides.repository;

import org.cardanofoundation.metadatatools.metafides.model.data.Account;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface AccountRepository extends ReactiveCrudRepository<Account, String> {
    @Query("SELECT * FROM account WHERE client_id = :#{[0]}")
    Flux<Account> findByClientId(final String clientId);
}
