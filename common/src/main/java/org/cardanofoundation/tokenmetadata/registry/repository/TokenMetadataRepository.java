package org.cardanofoundation.tokenmetadata.registry.repository;

import org.cardanofoundation.tokenmetadata.registry.entity.TokenMetadata;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!test")
public interface TokenMetadataRepository extends JpaRepository<TokenMetadata, String> {

}
