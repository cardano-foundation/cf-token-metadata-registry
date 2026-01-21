package org.cardanofoundation.tokenmetadata.registry.repository;

import org.cardanofoundation.tokenmetadata.registry.config.RepositoryTestConfig;
import org.cardanofoundation.tokenmetadata.registry.entity.MetadataReferenceNft;
import org.junit.jupiter.api.Test;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for MetadataReferenceNftRepository.
 * Tests the repository against an in-memory H2 database to verify the query behavior.
 */
@SpringBootTest(classes = RepositoryTestConfig.class)
@AutoConfigureTestDatabase
@Transactional
@ActiveProfiles("integration-test")
class MetadataReferenceNftRepositoryIT {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private MetadataReferenceNftRepository repository;

    @Test
    void findFirstByPolicyIdAndAssetNameOrderBySlotDesc_shouldReturnEntryWithMaxSlot() {
        // Given: Multiple entries with same policyId and assetName but different slots
        String policyId = "0c3b71e6468510341c813626cfa4d008d0b0c50d6ffcea188878e865";
        String assetName = "000643b050542d534e454b5f504552505f4c50";

        MetadataReferenceNft nft1 = MetadataReferenceNft.builder()
                .policyId(policyId)
                .assetName(assetName)
                .slot(172542832L)
                .name("PT-StrikeSNEK-Old")
                .description("Old description")
                .ticker("SNEK")
                .url("https://old-url.com")
                .decimals(6L)
                .logo("old-logo")
                .version(1L)
                .datum("old-datum")
                .build();

        MetadataReferenceNft nft2 = MetadataReferenceNft.builder()
                .policyId(policyId)
                .assetName(assetName)
                .slot(172547008L) // Higher slot number (latest)
                .name("PT-StrikeSNEK")
                .description("Latest description")
                .ticker("SNEK")
                .url("https://latest-url.com")
                .decimals(6L)
                .logo("latest-logo")
                .version(2L)
                .datum("latest-datum")
                .build();

        entityManager.persist(nft1);
        entityManager.persist(nft2);
        entityManager.flush();

        // When: Querying by policyId and assetName
        Optional<MetadataReferenceNft> result = repository.findFirstByPolicyIdAndAssetNameOrderBySlotDesc(policyId, assetName);

        // Then: Should return the entry with the highest slot
        assertTrue(result.isPresent(), "Should find a result");
        assertEquals(172547008L, result.get().getSlot(), "Should return entry with max slot");
        assertEquals("PT-StrikeSNEK", result.get().getName(), "Should return latest name");
        assertEquals("Latest description", result.get().getDescription(), "Should return latest description");
        assertEquals("https://latest-url.com", result.get().getUrl(), "Should return latest URL");
        assertEquals(2L, result.get().getVersion(), "Should return latest version");
    }

    @Test
    void findFirstByPolicyIdAndAssetNameOrderBySlotDesc_shouldReturnEmptyWhenNotFound() {
        // Given: No matching entries
        String policyId = "nonexistent";
        String assetName = "nonexistent";

        // When: Querying by policyId and assetName
        Optional<MetadataReferenceNft> result = repository.findFirstByPolicyIdAndAssetNameOrderBySlotDesc(policyId, assetName);

        // Then: Should return empty
        assertFalse(result.isPresent(), "Should not find a result");
    }

    @Test
    void findFirstByPolicyIdAndAssetNameOrderBySlotDesc_shouldWorkWithSingleEntry() {
        // Given: Single entry
        String policyId = "singlePolicy";
        String assetName = "singleAsset";

        MetadataReferenceNft nft = MetadataReferenceNft.builder()
                .policyId(policyId)
                .assetName(assetName)
                .slot(100000L)
                .name("Single Entry")
                .description("Single description")
                .ticker("SINGLE")
                .url("https://single.com")
                .decimals(6L)
                .logo("single-logo")
                .version(1L)
                .datum("single-datum")
                .build();

        entityManager.persist(nft);
        entityManager.flush();

        // When: Querying by policyId and assetName
        Optional<MetadataReferenceNft> result = repository.findFirstByPolicyIdAndAssetNameOrderBySlotDesc(policyId, assetName);

        // Then: Should return the single entry
        assertTrue(result.isPresent(), "Should find a result");
        assertEquals(100000L, result.get().getSlot(), "Should return the single entry");
        assertEquals("Single Entry", result.get().getName(), "Should return correct name");
    }

    @Test
    void findFirstByPolicyIdAndAssetNameOrderBySlotDesc_shouldHandleMultipleEntriesWithLargeSlotDifference() {
        // Given: Multiple entries with large slot differences
        String policyId = "largeGapPolicy";
        String assetName = "largeGapAsset";

        // Create entries with various slots in non-sequential order to ensure ordering works
        MetadataReferenceNft nft1 = createNft(policyId, assetName, 100000L, "First");
        MetadataReferenceNft nft2 = createNft(policyId, assetName, 999999999L, "Latest");
        MetadataReferenceNft nft3 = createNft(policyId, assetName, 500000L, "Middle");

        // Persist in random order to verify ordering happens in the query
        entityManager.persist(nft3);
        entityManager.persist(nft1);
        entityManager.persist(nft2);
        entityManager.flush();

        // When: Querying by policyId and assetName
        Optional<MetadataReferenceNft> result = repository.findFirstByPolicyIdAndAssetNameOrderBySlotDesc(policyId, assetName);

        // Then: Should return the entry with the highest slot regardless of insertion order
        assertTrue(result.isPresent(), "Should find a result");
        assertEquals(999999999L, result.get().getSlot(), "Should return entry with max slot");
        assertEquals("Latest", result.get().getName(), "Should return latest name");
    }

    @Test
    void findFirstByPolicyIdAndAssetNameOrderBySlotDesc_shouldDistinguishBetweenDifferentAssets() {
        // Given: Multiple assets with same policy but different asset names
        String policyId = "sharedPolicy";
        String assetName1 = "asset1";
        String assetName2 = "asset2";

        MetadataReferenceNft nft1 = createNft(policyId, assetName1, 200000L, "Asset1-Old");
        MetadataReferenceNft nft2 = createNft(policyId, assetName1, 300000L, "Asset1-New");
        MetadataReferenceNft nft3 = createNft(policyId, assetName2, 400000L, "Asset2");

        entityManager.persist(nft1);
        entityManager.persist(nft2);
        entityManager.persist(nft3);
        entityManager.flush();

        // When: Querying for asset1
        Optional<MetadataReferenceNft> result1 = repository.findFirstByPolicyIdAndAssetNameOrderBySlotDesc(policyId, assetName1);

        // Then: Should return only asset1's latest entry
        assertTrue(result1.isPresent(), "Should find asset1");
        assertEquals(300000L, result1.get().getSlot(), "Should return asset1's max slot");
        assertEquals("Asset1-New", result1.get().getName(), "Should return asset1's latest name");

        // When: Querying for asset2
        Optional<MetadataReferenceNft> result2 = repository.findFirstByPolicyIdAndAssetNameOrderBySlotDesc(policyId, assetName2);

        // Then: Should return only asset2's entry
        assertTrue(result2.isPresent(), "Should find asset2");
        assertEquals(400000L, result2.get().getSlot(), "Should return asset2's slot");
        assertEquals("Asset2", result2.get().getName(), "Should return asset2's name");
    }

    private MetadataReferenceNft createNft(String policyId, String assetName, Long slot, String name) {
        return MetadataReferenceNft.builder()
                .policyId(policyId)
                .assetName(assetName)
                .slot(slot)
                .name(name)
                .description("Description for " + name)
                .ticker("TKR")
                .url("https://example.com/" + name)
                .decimals(6L)
                .logo("logo-" + name)
                .version(1L)
                .datum("datum-" + name)
                .build();
    }
}
