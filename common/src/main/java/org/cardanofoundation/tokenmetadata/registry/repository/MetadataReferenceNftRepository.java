package org.cardanofoundation.tokenmetadata.registry.repository;

import org.cardanofoundation.tokenmetadata.registry.entity.MetadataReferenceNft;
import org.cardanofoundation.tokenmetadata.registry.entity.MetadataReferenceNftId;
import org.cardanofoundation.tokenmetadata.registry.entity.projections.MetadataReferenceNftProjection;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Profile("!test")
public interface MetadataReferenceNftRepository extends JpaRepository<MetadataReferenceNft, MetadataReferenceNftId> {

    @Query(value = """
            SELECT DISTINCT ON (policy_id, asset_name, slot) m.*
            FROM metadata_reference_nft m
            WHERE policy_id = :policyId
                AND asset_name = :assetName
            ORDER BY policy_id, asset_name, slot DESC
            """, nativeQuery = true)
    Optional<MetadataReferenceNftProjection> findByPolicyIdAndAssetName(@Param("policyId") String policyId, @Param("assetName") String assetName);

    @Query(value = """
            SELECT DISTINCT ON (policy_id, asset_name, slot) m.*
            FROM metadata_reference_nft m
            WHERE policy_id = :policyId
                AND asset_name = :assetName
            ORDER BY policy_id, asset_name, slot DESC
            """, nativeQuery = true)
    Optional<MetadataReferenceNft> findByPolicyIdAndAssetNameV2(@Param("policyId") String policyId, @Param("assetName") String assetName);

}
