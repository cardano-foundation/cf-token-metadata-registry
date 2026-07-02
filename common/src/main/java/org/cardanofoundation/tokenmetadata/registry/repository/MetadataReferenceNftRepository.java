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
     * Returns the latest reference NFT metadata per (policyId, assetName) for the given policy IDs.
     * Uses ROW_NUMBER() window function — portable across PostgreSQL, H2, and MySQL 8+.
     */
    @Query(value = "SELECT * FROM (SELECT *, ROW_NUMBER() OVER (PARTITION BY policy_id, asset_name ORDER BY slot DESC) AS rn " +
            "FROM metadata_reference_nft WHERE policy_id IN (:policyIds)) ranked WHERE rn = 1",
            nativeQuery = true)
    List<MetadataReferenceNft> findLatestByPolicyIds(@Param("policyIds") Collection<String> policyIds);

    int deleteBySlotGreaterThan(Long slot);

}
