package org.cardanofoundation.tokenmetadata.registry.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.entity.TokenLogo;
import org.cardanofoundation.tokenmetadata.registry.entity.TokenMetadata;
import org.cardanofoundation.tokenmetadata.registry.invalidtoken.InvalidTokenReason;
import org.cardanofoundation.tokenmetadata.registry.invalidtoken.InvalidTokenRecorder;
import org.cardanofoundation.tokenmetadata.registry.invalidtoken.InvalidTokenReport;
import org.cardanofoundation.tokenmetadata.registry.model.Mapping;
import org.cardanofoundation.tokenmetadata.registry.repository.TokenLogoRepository;
import org.cardanofoundation.tokenmetadata.registry.repository.TokenMetadataRepository;
import org.cardanofoundation.tokenmetadata.registry.util.Cip26Validation;
import org.cardanofoundation.tokenmetadata.registry.util.MappingsUtil;
import org.cardanofoundation.tokenmetadata.registry.util.TokenMetadataValidator;
import org.cardanofoundation.tokenmetadata.registry.validation.ValidationError;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.cardanofoundation.tokenmetadata.registry.util.MappingsUtil.toTokenLogo;

@Service
@Slf4j
@AllArgsConstructor
public class TokenMetadataService {

    private final TokenMetadataRepository tokenMetadataRepository;
    private final TokenLogoRepository tokenLogoRepository;
    private final TokenMetadataValidator tokenMetadataValidator;
    private final InvalidTokenRecorder invalidTokenRecorder;

    /**
     * Stores one registry entry and its logo. An entry that breaks CIP-26 is stored as written, with its validation errors
     * (the logo's included), and served (ADR-019). Only an entry whose subject cannot be a key is dropped: it is recorded
     * in {@code invalid_token} under its registry file. A logo far beyond any real size is not stored in the logo table, with
     * a warning; the entry is.
     *
     * @param origin the registry file the entry comes from
     * @return true if the entry was stored, false if it was dropped or saving it failed
     */
    public boolean insertMapping(Mapping mapping, LocalDateTime updatedAt, String updateBy, String origin) {
        TokenMetadata tokenMetadata = MappingsUtil.toTokenMetadata(mapping, updateBy, updatedAt);

        Cip26Validation validation = tokenMetadataValidator.validate(mapping, tokenMetadata);
        if (validation.isDropped()) {
            log.warn("Dropping CIP-26 entry '{}' of '{}': {}", tokenMetadata.getSubject(), origin, validation.droppedReason());
            invalidTokenRecorder.report(InvalidTokenReport.builder()
                    .standard(InvalidTokenReason.CIP_26)
                    .reasonCode(InvalidTokenReason.UNUSABLE_SUBJECT)
                    .reason(validation.droppedReason())
                    .subject(mapping.subject())
                    .origin(origin)
                    .build());
            return false;
        }

        TokenLogo tokenLogo = toTokenLogo(mapping);
        Cip26Validation logoValidation = tokenMetadataValidator.validateLogo(tokenMetadata.getSubject(), tokenLogo.getLogo());
        List<ValidationError> errors = new ArrayList<>(validation.errors());
        errors.addAll(logoValidation.errors());
        tokenMetadata.setValidationErrors(errors);

        try {
            tokenMetadataRepository.save(tokenMetadata);
            if (tokenLogo.getLogo() != null && !tokenLogo.getLogo().isEmpty() && !logoValidation.isDropped()) {
                tokenLogoRepository.save(tokenLogo);
            } else {
                // a logo that is gone or dropped must not leave the logo of an earlier version behind
                tokenLogoRepository.deleteById(tokenMetadata.getSubject());
            }
            return true;
        } catch (Exception e) {
            log.error("Failed to save token metadata for subject '{}': {}", tokenMetadata.getSubject(), e.getMessage());
            return false;
        }
    }

    /**
     * Deletes the metadata (and its logo, which has a foreign key on metadata) for a subject whose mapping file was
     * removed from the upstream registry. Deleting a subject that is not present locally is a no-op.
     *
     * @return true if the deletion succeeded (including the no-op case), false if an error occurred
     */
    public boolean deleteMapping(String subject) {
        try {
            // the subject comes from a file name, stored entries are in lower case
            String storedSubject = MappingsUtil.normalizeSubject(subject);
            tokenLogoRepository.deleteById(storedSubject);
            tokenMetadataRepository.deleteById(storedSubject);
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

}
