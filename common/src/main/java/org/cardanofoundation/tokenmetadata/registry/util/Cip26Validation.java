package org.cardanofoundation.tokenmetadata.registry.util;

import jakarta.annotation.Nullable;
import org.cardanofoundation.tokenmetadata.registry.validation.ValidationError;

import java.util.List;

/**
 * The outcome of checking a registry entry (or its logo) against CIP-26: the {@link ValidationError}s to store and
 * return with it, and, only for what cannot be stored at all or is an obvious attack, the reason it is dropped.
 *
 * @param errors        how the entry departs from CIP-26; the entry is stored and served whatever they are
 * @param droppedReason why the entry (or the logo) is dropped, or {@code null} when it is kept
 */
public record Cip26Validation(List<ValidationError> errors, @Nullable String droppedReason) {

    public static Cip26Validation of(List<ValidationError> errors) {
        return new Cip26Validation(errors, null);
    }

    public static Cip26Validation dropped(String reason) {
        return new Cip26Validation(List.of(), reason);
    }

    public boolean isDropped() {
        return droppedReason != null;
    }
}
