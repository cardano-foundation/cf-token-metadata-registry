package org.cardanofoundation.tokenmetadata.registry.util;

import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.entity.TokenMetadata;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Validator to ensure token metadata fields meet database schema constraints.
 */
@Component
@Slf4j
public class TokenMetadataValidator {

    // Database schema constraints from V0__metadata_server_db_init.sql
    private static final int MAX_SUBJECT_LENGTH = 255;
    private static final int MAX_NAME_LENGTH = 255;
    private static final int MAX_TICKER_LENGTH = 32;
    private static final int MAX_URL_LENGTH = 255;
    private static final int MAX_UPDATED_BY_LENGTH = 255;

    /**
     * Validates that all fields in the TokenMetadata entity comply with database constraints.
     *
     * @param tokenMetadata the token metadata to validate
     * @return true if valid, false otherwise
     */
    public boolean validate(TokenMetadata tokenMetadata) {
        List<String> violations = new ArrayList<>();

        if (exceedsLength(tokenMetadata.getSubject(), MAX_SUBJECT_LENGTH)) {
            violations.add(String.format("subject exceeds %d characters (length: %d)",
                    MAX_SUBJECT_LENGTH, tokenMetadata.getSubject().length()));
        }

        if (exceedsLength(tokenMetadata.getName(), MAX_NAME_LENGTH)) {
            violations.add(String.format("name exceeds %d characters (length: %d)",
                    MAX_NAME_LENGTH, tokenMetadata.getName().length()));
        }

        if (exceedsLength(tokenMetadata.getTicker(), MAX_TICKER_LENGTH)) {
            violations.add(String.format("ticker exceeds %d characters (length: %d)",
                    MAX_TICKER_LENGTH, tokenMetadata.getTicker().length()));
        }

        if (exceedsLength(tokenMetadata.getUrl(), MAX_URL_LENGTH)) {
            violations.add(String.format("url exceeds %d characters (length: %d)",
                    MAX_URL_LENGTH, tokenMetadata.getUrl().length()));
        }

        if (exceedsLength(tokenMetadata.getUpdatedBy(), MAX_UPDATED_BY_LENGTH)) {
            violations.add(String.format("updatedBy exceeds %d characters (length: %d)",
                    MAX_UPDATED_BY_LENGTH, tokenMetadata.getUpdatedBy().length()));
        }

        if (!violations.isEmpty()) {
            log.warn("Token metadata validation failed for subject '{}': {}",
                    tokenMetadata.getSubject(), String.join(", ", violations));
            return false;
        }

        return true;
    }

    private boolean exceedsLength(String value, int maxLength) {
        return value != null && value.length() > maxLength;
    }
}
