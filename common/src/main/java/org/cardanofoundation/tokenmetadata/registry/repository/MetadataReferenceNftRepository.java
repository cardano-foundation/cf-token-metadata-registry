package org.cardanofoundation.tokenmetadata.registry.repository;

import org.cardanofoundation.tokenmetadata.registry.entity.MetadataReferenceNft;
import org.cardanofoundation.tokenmetadata.registry.entity.MetadataReferenceNftId;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("!test")
public interface MetadataReferenceNftRepository extends JpaRepository<MetadataReferenceNft, MetadataReferenceNftId> {

    /**
     * Returns the most recent metadata (highest slot) for a given policyId and assetName.
     * Uses Spring Data JPA method naming convention to order by slot descending and return first result.
     */
    Optional<MetadataReferenceNft> findFirstByPolicyIdAndAssetNameOrderBySlotDesc(String policyId, String assetName);

    /**
     * Returns the latest CIP-68 reference NFT per policy ID using PostgreSQL DISTINCT ON.
     * Used for enriching CIP-113 registry entries with display metadata.
     */
    @Query(value = "SELECT DISTINCT ON (policy_id) * FROM metadata_reference_nft " +
            "WHERE policy_id IN :policyIds ORDER BY policy_id, slot DESC",
            nativeQuery = true)
    List<MetadataReferenceNft> findLatestByPolicyIds(@Param("policyIds") Collection<String> policyIds);

}
