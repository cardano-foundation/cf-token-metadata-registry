package org.cardanofoundation.tokenmetadata.registry.util;

import lombok.experimental.UtilityClass;

import java.util.Base64;
import java.util.Optional;

@UtilityClass
public class Base64Utils {

    /**
     * Decodes {@code value} as standard RFC 4648 base64 ({@link Base64#getDecoder()}).
     *
     * @return the decoded bytes, or empty if {@code value} is not valid base64
     */
    public Optional<byte[]> decode(String value) {
        try {
            return Optional.of(Base64.getDecoder().decode(value));
        } catch (IllegalArgumentException _) {
            return Optional.empty();
        }
    }
}
