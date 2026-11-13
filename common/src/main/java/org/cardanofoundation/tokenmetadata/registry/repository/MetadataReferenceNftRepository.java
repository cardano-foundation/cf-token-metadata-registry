package org.cardanofoundation.tokenmetadata.registry.repository;

import org.cardanofoundation.tokenmetadata.registry.entity.MetadataReferenceNft;
import org.cardanofoundation.tokenmetadata.registry.entity.MetadataReferenceNftId;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
@Profile("!test")
public interface MetadataReferenceNftRepository extends JpaRepository<MetadataReferenceNft, MetadataReferenceNftId> {

    /**
     * Returns the most recent metadata (highest slot) for a given policyId and assetName.
     * Uses Spring Data JPA method naming convention to order by slot descending and return first result.
     */
    Optional<MetadataReferenceNft> findFirstByPolicyIdAndAssetNameOrderBySlotDesc(String policyId, String assetName);

    /** A policy ID is a 28-byte hash, 56 hex characters. */
    int POLICY_ID_HEX_LENGTH = 56;

    /** Most keys {@link #findLatestByUnits} sends in one query; larger requests are split. */
    int MAX_KEYS_PER_QUERY = 1000;

    /**
     * Returns the latest metadata per requested reference NFT, in one query per {@value #MAX_KEYS_PER_QUERY} units.
     * <p>
     * Each unit ({@code policyId || assetName}) is split back into its policy ID and asset name, and the query filters
     * on both, so the database reads only the requested reference NFTs through the index on
     * {@code (policy_id, asset_name, slot)}. A filter on the policy IDs alone reads every reference NFT of those
     * policies, however few of their subjects were asked for: a policy can have tens of thousands of stored datums
     * (an NFT collection whose datums are stored, though never served, ADR-019), read with a sequential scan and
     * sorted on disk.
     * <p>
     * Large requests are split, so that a query binds at most three parameters per unit, far below the 65,535 bind
     * parameters a PostgreSQL statement accepts, and the pairs the two {@code IN} lists can match stay bounded.
     *
     * @param units {@code policyId || assetName} of the reference NFTs; units shorter than a policy ID are skipped
     * @return one row per requested unit that has metadata; units without any are omitted
     */
    default List<MetadataReferenceNft> findLatestByUnits(Collection<String> units) {
        List<String> keys = units.stream()
                .filter(unit -> unit.length() >= POLICY_ID_HEX_LENGTH)
                .distinct()
                .toList();

        List<MetadataReferenceNft> rows = new ArrayList<>();
        for (int from = 0; from < keys.size(); from += MAX_KEYS_PER_QUERY) {
            List<String> chunk = keys.subList(from, Math.min(from + MAX_KEYS_PER_QUERY, keys.size()));
            Set<String> policyIds = new HashSet<>();
            Set<String> assetNames = new HashSet<>();
            for (String unit : chunk) {
                policyIds.add(unit.substring(0, POLICY_ID_HEX_LENGTH));
                assetNames.add(unit.substring(POLICY_ID_HEX_LENGTH));
            }
            rows.addAll(findLatestByPolicyIdsAndAssetNames(policyIds, assetNames, chunk));
        }
        return rows;
    }

    /**
     * Latest row per requested reference NFT. Use {@link #findLatestByUnits}, which derives the three arguments from
     * the units and splits large requests. {@code policyIds} and {@code assetNames} select the rows through the index;
     * they also match pairs that were not asked for (a requested policy with an asset name requested for another
     * policy), which {@code CONCAT(policy_id, asset_name) IN (:units)} drops before the rows are ranked.
     * Uses ROW_NUMBER() window function — portable across PostgreSQL, H2, and MySQL 8+.
     */
    @Query(value = "SELECT * FROM (SELECT *, ROW_NUMBER() OVER (PARTITION BY policy_id, asset_name ORDER BY slot DESC) AS rn " +
            "FROM metadata_reference_nft WHERE policy_id IN (:policyIds) AND asset_name IN (:assetNames) " +
            "AND CONCAT(policy_id, asset_name) IN (:units)) ranked WHERE rn = 1",
            nativeQuery = true)
    List<MetadataReferenceNft> findLatestByPolicyIdsAndAssetNames(@Param("policyIds") Collection<String> policyIds,
                                                                  @Param("assetNames") Collection<String> assetNames,
                                                                  @Param("units") Collection<String> units);

    /** Whether any version of the reference NFT's metadata is stored. */
    boolean existsByPolicyIdAndAssetName(String policyId, String assetName);

    int deleteBySlotGreaterThan(Long slot);

}
