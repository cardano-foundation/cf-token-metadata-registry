package org.cardanofoundation.tokenmetadata.registry.repository;

import org.cardanofoundation.tokenmetadata.registry.entity.TokenMetadata;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
@Profile("!test")
public interface TokenMetadataRepository extends JpaRepository<TokenMetadata, String> {

    @Query("select t.subject from TokenMetadata t")
    List<String> findAllSubjects();

    /** The policies, among the given ones, that have a CIP-26 entry for one of their tokens (a subject starts with its policy id). */
    @Query("select distinct substring(t.subject, 1, 56) from TokenMetadata t where substring(t.subject, 1, 56) in :policyIds")
    List<String> findPolicyIdsWithMetadata(@Param("policyIds") Collection<String> policyIds);

}
