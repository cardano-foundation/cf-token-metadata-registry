package org.cardanofoundation.tokenmetadata.registry.repository;

import org.cardanofoundation.tokenmetadata.registry.entity.TokenMetadata;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
@Profile("!test")
public interface TokenMetadataRepository extends JpaRepository<TokenMetadata, String> {

    List<TokenMetadata> findByPolicy(String policy);

    List<TokenMetadata> findByPolicyIn(Collection<String> policies);

}
