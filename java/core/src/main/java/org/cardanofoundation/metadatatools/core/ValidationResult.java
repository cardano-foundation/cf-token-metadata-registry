package org.cardanofoundation.metadatatools.core;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.PUBLIC)
@NoArgsConstructor
@ToString
public class ValidationResult {
    private boolean valid = true;
    private final List<String> validationErrors = new ArrayList<>();

    public void addValidationError(final String error) {
        if (error == null) {
            throw new IllegalArgumentException("error cannot be null.");
        }
        if (error.isEmpty() || error.isBlank()) {
            throw new IllegalArgumentException("error cannot be empty or blank.");
        }
        this.validationErrors.add(error);
        this.valid = false;
    }

    public void clearValidationErrors() {
        this.validationErrors.clear();
        this.valid = true;
    }

    public void mergeWith(final ValidationResult otherResult) {
        this.valid = this.valid && otherResult.isValid();
        this.validationErrors.addAll(otherResult.getValidationErrors());
    }

    public static ValidationResult mergeResults(final List<ValidationResult> validationResults) {
        final ValidationResult mergedResult = new ValidationResult();
        if (!validationResults.isEmpty()) {
            for (final ValidationResult result : validationResults) {
                mergedResult.mergeWith(result);
            }
        }
        return mergedResult;
    }
}
