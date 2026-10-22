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

    /** Whether any version of the reference NFT's metadata is stored. */
    boolean existsByPolicyIdAndAssetName(String policyId, String assetName);

    int deleteBySlotGreaterThan(Long slot);

    /**
     * The policies, among the given ones, that have CIP-68 metadata which is served: a reference NFT whose 333 or 444
     * user token was seen on chain (ADR-022). A reference NFT alone serves nothing.
     */
    @Query("""
            SELECT DISTINCT m.policyId FROM MetadataReferenceNft m
            WHERE m.policyId IN :policyIds
              AND EXISTS (SELECT 1 FROM Cip68UserToken u
                          WHERE u.unit = CONCAT(m.policyId, '0014df10', SUBSTRING(m.assetName, 9))
                             OR u.unit = CONCAT(m.policyId, '001bc280', SUBSTRING(m.assetName, 9)))
            """)
    List<String> findPolicyIdsWithServedMetadata(@Param("policyIds") Collection<String> policyIds);

}
