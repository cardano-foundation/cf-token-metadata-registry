package org.cardanofoundation.tokenmetadata.registry.util;

import jakarta.annotation.Nullable;
import org.cardanofoundation.tokenmetadata.registry.invalidtoken.InvalidTokenReason;
import org.cardanofoundation.tokenmetadata.registry.validation.ValidationError;

import java.util.List;

/**
 * The outcome of checking a registry entry (or its logo) against CIP-26: the {@link ValidationError}s to store and
 * return with it, and, only for what cannot be stored at all or is an obvious attack, the reason it is dropped.
 *
 * @param errors        how the entry departs from CIP-26; the entry is stored and served whatever they are
 * @param droppedCode   the {@code invalid_token} reason code of a drop ({@link InvalidTokenReason}), or {@code null} when
 *                      it is kept
 * @param droppedReason why the entry (or the logo) is dropped, or {@code null} when it is kept
 */
public record Cip26Validation(List<ValidationError> errors, @Nullable String droppedCode, @Nullable String droppedReason) {

    public static Cip26Validation of(List<ValidationError> errors) {
        return new Cip26Validation(errors, null, null);
    }

    /**
     * @param reasonCode the {@code invalid_token} reason code, see {@link InvalidTokenReason}
     * @param reason     what is wrong, for people
     */
    public static Cip26Validation dropped(String reasonCode, String reason) {
        return new Cip26Validation(List.of(), reasonCode, reason);
    }

    public boolean isDropped() {
        return droppedReason != null;
    }
}
