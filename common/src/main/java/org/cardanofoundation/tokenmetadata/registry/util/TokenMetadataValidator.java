package org.cardanofoundation.tokenmetadata.registry.util;

import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.metadatatools.core.cip26.MetadataCreator;
import org.cardanofoundation.metadatatools.core.cip26.ValidationError;
import org.cardanofoundation.metadatatools.core.cip26.ValidationField;
import org.cardanofoundation.metadatatools.core.cip26.ValidationResult;
import org.cardanofoundation.metadatatools.core.cip26.model.Metadata;
import org.cardanofoundation.metadatatools.core.cip26.model.MetadataProperty;
import org.cardanofoundation.tokenmetadata.registry.entity.TokenMetadata;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Validator to ensure token metadata fields meet CIP-26 specification constraints.
 * This validator uses the cf-metadata-core library's MetadataCreator and MetadataValidationRules
 * to enforce CIP-26 compliance.
 */
@Component
@Slf4j
public class TokenMetadataValidator {

    /**
     * Validates that all fields in the TokenMetadata entity comply with CIP-26 specification
     * using the cf-metadata-core library's validation rules.
     *
     * @param tokenMetadata the token metadata to validate
     * @return true if valid according to CIP-26, false otherwise
     */
    public boolean validate(TokenMetadata tokenMetadata) {
        try {
            // Convert registry TokenMetadata to cf-metadata-core Metadata
            Metadata cip26Metadata = convertToMetadata(tokenMetadata);

            // Use MetadataCreator from cf-metadata-core library to validate
            ValidationResult validationResult = MetadataCreator.validateMetadata(cip26Metadata);

            if (!validationResult.isValid()) {
                String errorMessages = validationResult.getValidationErrors().stream()
                        .map(error -> error.getField() + ": " + error.getMessage())
                        .collect(Collectors.joining(", "));
                log.warn("CIP-26 validation failed for subject '{}': {}",
                        tokenMetadata.getSubject(), errorMessages);
                return false;
            }

            return true;

        } catch (Exception e) {
            log.error("Error during CIP-26 validation for subject '{}': {}",
                    tokenMetadata.getSubject(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Validates a logo string according to CIP-26 specification.
     * Logo is optional but if present must comply with CIP-26 constraints (max 87,400 characters).
     *
     * @param subject the token subject (for logging)
     * @param logo the logo string to validate (can be null)
     * @return true if valid according to CIP-26, false otherwise
     */
    public boolean validateLogo(String subject, String logo) {
        // Logo is optional, so null is valid
        if (logo == null || logo.isEmpty()) {
            return true;
        }

        try {
            // Create a minimal Metadata object just for logo validation
            Metadata metadata = new Metadata();
            metadata.setSubject(subject);

            // Add required fields with dummy values (needed for Metadata validation)
            metadata.addProperty(ValidationField.NAME, new MetadataProperty<>("dummy"));
            metadata.addProperty(ValidationField.DESCRIPTION, new MetadataProperty<>("dummy"));

            // Add logo property for validation
            metadata.addProperty(ValidationField.LOGO, new MetadataProperty<>(logo));

            // Validate using cf-metadata-core library
            ValidationResult validationResult = MetadataCreator.validateMetadata(metadata);

            // Check for logo-related or subject-related errors using ValidationField enum
            // We need to validate both logo and subject because logo insertion requires a valid subject
            if (!validationResult.isValid()) {
                List<ValidationError> logoErrors = validationResult.getValidationErrorsForField(ValidationField.LOGO);
                List<ValidationError> subjectErrors = validationResult.getValidationErrorsForField(ValidationField.SUBJECT);

                if (!logoErrors.isEmpty() || !subjectErrors.isEmpty()) {
                    List<ValidationError> relevantErrors = new ArrayList<>(logoErrors);
                    relevantErrors.addAll(subjectErrors);

                    String errorMessages = relevantErrors.stream()
                            .map(error -> error.getField() + ": " + error.getMessage())
                            .collect(Collectors.joining(", "));

                    log.warn("CIP-26 logo validation failed for subject '{}': {}",
                            subject, errorMessages);
                    return false;
                }
            }

            return true;

        } catch (Exception e) {
            log.error("Error during CIP-26 logo validation for subject '{}': {}",
                    subject, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Converts registry TokenMetadata entity to cf-metadata-core Metadata model.
     */
    private Metadata convertToMetadata(TokenMetadata tokenMetadata) {
        Metadata metadata = new Metadata();

        // Set subject
        if (tokenMetadata.getSubject() != null) {
            metadata.setSubject(tokenMetadata.getSubject());
        }

        // Add required properties: name and description
        if (tokenMetadata.getName() != null) {
            metadata.addProperty(ValidationField.NAME, new MetadataProperty<>(tokenMetadata.getName()));
        }

        if (tokenMetadata.getDescription() != null) {
            metadata.addProperty(ValidationField.DESCRIPTION, new MetadataProperty<>(tokenMetadata.getDescription()));
        }

        // Add optional properties
        if (tokenMetadata.getTicker() != null) {
            metadata.addProperty(ValidationField.TICKER, new MetadataProperty<>(tokenMetadata.getTicker()));
        }

        if (tokenMetadata.getDecimals() != null) {
            metadata.addProperty(ValidationField.DECIMALS, new MetadataProperty<>(tokenMetadata.getDecimals().intValue()));
        }

        if (tokenMetadata.getUrl() != null) {
            metadata.addProperty(ValidationField.URL, new MetadataProperty<>(tokenMetadata.getUrl()));
        }

        // Logo is not stored in TokenMetadata directly, only in TokenLogo table
        // Logo validation is done separately via validateLogo() method

        return metadata;
    }
}
