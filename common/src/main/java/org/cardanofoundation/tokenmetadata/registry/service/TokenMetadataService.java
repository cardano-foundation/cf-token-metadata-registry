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
     * Validates field lengths before insertion.
     *
     * @return true if successfully inserted, false if validation failed
     */
    public boolean insertMapping(Mapping mapping, LocalDateTime updatedAt, String updateBy) {
        var tokenMetadata = MappingsUtil.toTokenMetadata(mapping, updateBy, updatedAt);

        if (!tokenMetadataValidator.validate(tokenMetadata)) {
            log.warn("Skipping token metadata insertion for subject '{}' due to validation failure",
                    tokenMetadata.getSubject());
            return false;
        }

        tokenMetadataRepository.save(tokenMetadata);

        return true;
    }

    /**
     * Inserts logo data into the database.
     *
     * @return true if successfully inserted, false if validation failed
     */
    public boolean insertLogo(Mapping mapping) {
        var tokenLogo = toTokenLogo(mapping);

        // Validate subject length (logo table has FK to metadata table with varchar(255))
        if (tokenLogo.getSubject() != null && tokenLogo.getSubject().length() > 255) {
            log.warn("Skipping logo insertion for subject '{}' - subject exceeds 255 characters (length: {})",
                    tokenLogo.getSubject(), tokenLogo.getSubject().length());
            return false;
        }

        tokenLogoRepository.save(tokenLogo);

        return true;
    }

}
