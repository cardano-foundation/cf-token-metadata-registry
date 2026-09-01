package org.cardanofoundation.tokenmetadata.registry.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.entity.TokenLogo;
import org.cardanofoundation.tokenmetadata.registry.entity.TokenMetadata;
import org.cardanofoundation.tokenmetadata.registry.model.Mapping;
import org.cardanofoundation.tokenmetadata.registry.repository.TokenLogoRepository;
import org.cardanofoundation.tokenmetadata.registry.repository.TokenMetadataRepository;
import org.cardanofoundation.tokenmetadata.registry.util.MappingsUtil;
import org.cardanofoundation.tokenmetadata.registry.util.TokenMetadataValidator;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static org.cardanofoundation.tokenmetadata.registry.util.MappingsUtil.toTokenLogo;

@Service
@Slf4j
@AllArgsConstructor
public class TokenMetadataService {

    private final TokenMetadataRepository tokenMetadataRepository;

    private final TokenLogoRepository tokenLogoRepository;

    private final TokenMetadataValidator tokenMetadataValidator;

    /**
     * Inserts mapping metadata into the database.
     * Validates metadata before insertion.
     *
     * @return true if successfully inserted, false if validation failed or error occurred
     */
    public boolean insertMapping(Mapping mapping, LocalDateTime updatedAt, String updateBy) {
        TokenMetadata tokenMetadata = MappingsUtil.toTokenMetadata(mapping, updateBy, updatedAt);

        if (!tokenMetadataValidator.validate(tokenMetadata)) {
            log.warn("Skipping token metadata for subject '{}' - validation failed", tokenMetadata.getSubject());
            return false;
        }

        try {
            tokenMetadataRepository.save(tokenMetadata);
            return true;
        } catch (Exception e) {
            log.error("Failed to save token metadata for subject '{}': {}", tokenMetadata.getSubject(), e.getMessage());
            return false;
        }
    }

    /**
     * Deletes the metadata (and its logo, which has a foreign key on metadata) for a subject
     * whose mapping file was removed from the upstream registry. Deleting a subject that is
     * not present locally is a no-op.
     *
     * @return true if the deletion succeeded (including the no-op case), false if an error occurred
     */
    public boolean deleteMapping(String subject) {
        try {
            tokenLogoRepository.deleteById(subject);
            tokenMetadataRepository.deleteById(subject);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete token metadata for subject '{}': {}", subject, e.getMessage());
            return false;
        }
    }

    /**
     * @return subjects of all CIP-26 metadata rows currently stored locally
     */
    public List<String> findAllSubjects() {
        return tokenMetadataRepository.findAllSubjects();
    }

    /**
     * Inserts logo data into the database.
     * Validates logo according to CIP-26 before insertion.
     *
     * @return true if successfully inserted, false if validation failed or error occurred
     */
    public boolean insertLogo(Mapping mapping) {
        TokenLogo tokenLogo = toTokenLogo(mapping);

        if (!tokenMetadataValidator.validateLogo(tokenLogo.getSubject(), tokenLogo.getLogo())) {
            log.warn("Skipping logo for subject '{}' - validation failed", tokenLogo.getSubject());
            return false;
        }

        try {
            tokenLogoRepository.save(tokenLogo);
            return true;
        } catch (Exception e) {
            log.error("Failed to save logo for subject '{}': {}", tokenLogo.getSubject(), e.getMessage());
            return false;
        }
    }

}
