package org.cardanofoundation.tokenmetadata.registry.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.cardanofoundation.tokenmetadata.registry.config.RepositoryTestConfig;
import org.cardanofoundation.tokenmetadata.registry.entity.InvalidToken;
import org.cardanofoundation.tokenmetadata.registry.entity.MetadataReferenceNft;
import org.cardanofoundation.tokenmetadata.registry.entity.TokenMetadata;
import org.cardanofoundation.tokenmetadata.registry.validation.ValidationError;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The columns and queries of ADR-019 and ADR-020 against a real database: validation errors and properties as JSON, the
 * nullable CIP-68 name and version, the policy queries of the CIP-113 reconciliation, and the invalid_token table.
 */
@SpringBootTest(classes = RepositoryTestConfig.class)
@AutoConfigureTestDatabase
@Transactional
@ActiveProfiles("integration-test")
class ValidationErrorsRepositoryIT {

    private static final String POLICY_A = "aa".repeat(28);
    private static final String POLICY_B = "bb".repeat(28);
    private static final String POLICY_C = "cc".repeat(28);
    private static final ValidationError ERROR = ValidationError.mandatory("CIP-68", "333", "description", "missing_value",
            "description field is missing a mandatory value");

    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private MetadataReferenceNftRepository metadataReferenceNftRepository;
    @Autowired
    private TokenMetadataRepository tokenMetadataRepository;
    @Autowired
    private InvalidTokenRepository invalidTokenRepository;

    @Test
    void storesTheValidationErrorsAndPropertiesOfACip68RowWithoutNameOrVersion() {
        metadataReferenceNftRepository.save(MetadataReferenceNft.builder()
                .policyId(POLICY_A).assetName("000643b041").slot(1L).datum("d87980")
                .image("ipfs://bafy").mediaType("image/png")
                .properties(Map.of("additional_properties", Map.of("website", "https://x.io")))
                .validationErrors(List.of(ERROR))
                .build());
        entityManager.flush();
        entityManager.clear();

        MetadataReferenceNft read = metadataReferenceNftRepository.findFirstByPolicyIdAndAssetNameOrderBySlotDesc(POLICY_A, "000643b041").orElseThrow();
        assertThat(read.getName()).isNull();
        assertThat(read.getVersion()).isNull();
        assertThat(read.getImage()).isEqualTo("ipfs://bafy");
        assertThat(read.getValidationErrors()).containsExactly(ERROR);
        assertThat(read.getProperties()).containsKey("additional_properties");
        assertThat(metadataReferenceNftRepository.existsByPolicyIdAndAssetName(POLICY_A, "000643b041")).isTrue();
        assertThat(metadataReferenceNftRepository.existsByPolicyIdAndAssetName(POLICY_A, "000643b042")).isFalse();
    }

    @Test
    void storesTheValidationErrorsOfACip26EntryAndTellsNeverCheckedFromNoErrors() {
        tokenMetadataRepository.save(cip26(POLICY_A + "41", List.of(ERROR)));
        tokenMetadataRepository.save(cip26(POLICY_B + "41", List.of()));
        tokenMetadataRepository.save(cip26(POLICY_C + "41", null));
        entityManager.flush();
        entityManager.clear();

        assertThat(tokenMetadataRepository.findById(POLICY_A + "41").orElseThrow().getValidationErrors()).containsExactly(ERROR);
        assertThat(tokenMetadataRepository.findById(POLICY_B + "41").orElseThrow().getValidationErrors()).isEmpty();
        assertThat(tokenMetadataRepository.findById(POLICY_C + "41").orElseThrow().getValidationErrors()).isNull();
    }

    @Test
    void findsThePoliciesThatHaveCip26OrCip68Metadata() {
        tokenMetadataRepository.save(cip26(POLICY_A + "41", List.of()));
        metadataReferenceNftRepository.save(MetadataReferenceNft.builder().policyId(POLICY_B).assetName("000643b041").slot(1L)
                .name("T").version(1L).datum("d87980").build());
        entityManager.flush();

        List<String> queried = List.of(POLICY_A, POLICY_B, POLICY_C);
        assertThat(tokenMetadataRepository.findPolicyIdsWithMetadata(queried)).containsExactly(POLICY_A);
        assertThat(metadataReferenceNftRepository.findPolicyIdsWithMetadata(queried)).containsExactly(POLICY_B);
    }

    @Test
    void keepsInvalidTokensUntilARollbackOrTheirFileIsJudgedAgain() {
        invalidTokenRepository.save(invalid("onchain-1", "CIP-68", "datum_undecodable", 100L, null));
        invalidTokenRepository.save(invalid("onchain-2", "CIP-68", "datum_undecodable", 200L, null));
        invalidTokenRepository.save(invalid("file-1", "CIP-26", "filename_subject_mismatch", null, "garbage.json"));
        invalidTokenRepository.save(invalid("cip113-1", "CIP-113", "no_companion_metadata", null, null));
        entityManager.flush();

        assertThat(invalidTokenRepository.deleteBySlotGreaterThan(150L)).isEqualTo(1);
        assertThat(invalidTokenRepository.deleteByStandardAndOrigin("CIP-26", "garbage.json")).isEqualTo(1);
        assertThat(invalidTokenRepository.findByStandardAndReasonCode("CIP-113", "no_companion_metadata"))
                .extracting(InvalidToken::getId).containsExactly("cip113-1");
        assertThat(invalidTokenRepository.deleteByIdIn(List.of("cip113-1"))).isEqualTo(1);
        entityManager.clear();
        assertThat(invalidTokenRepository.findAll()).extracting(InvalidToken::getId).containsExactly("onchain-1");
        assertThat(invalidTokenRepository.findById("onchain-1").orElseThrow().getDetails()).containsEntry("datum_prefix", "dead");
    }

    private static TokenMetadata cip26(String subject, List<ValidationError> errors) {
        TokenMetadata metadata = new TokenMetadata();
        metadata.setSubject(subject);
        metadata.setName("Token");
        metadata.setValidationErrors(errors);
        return metadata;
    }

    private static InvalidToken invalid(String id, String standard, String reasonCode, Long slot, String origin) {
        return InvalidToken.builder().id(id).standard(standard).reasonCode(reasonCode).reason("r").slot(slot).origin(origin)
                .details(Map.of("datum_prefix", "dead")).build();
    }
}
