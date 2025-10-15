package org.cardanofoundation.tokenmetadata.registry.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.model.Mapping;
import org.cardanofoundation.tokenmetadata.registry.repository.TokenLogoRepository;
import org.cardanofoundation.tokenmetadata.registry.repository.TokenMetadataRepository;
import org.cardanofoundation.tokenmetadata.registry.util.MappingsUtil;
import org.cardanofoundation.tokenmetadata.registry.util.TokenMetadataValidator;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

import static org.cardanofoundation.tokenmetadata.registry.util.MappingsUtil.toTokenLogo;

@Component
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
        var tokenMetadata = MappingsUtil.toTokenMetadata(mapping, updateBy, updatedAt);

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
     * Inserts logo data into the database.
     * Validates logo according to CIP-26 before insertion.
     *
     * @return true if successfully inserted, false if validation failed or error occurred
     */
    public boolean insertLogo(Mapping mapping) {
        var tokenLogo = toTokenLogo(mapping);

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
