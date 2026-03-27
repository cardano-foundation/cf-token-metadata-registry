package org.cardanofoundation.tokenmetadata.registry.api.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LogSanitizer")
class LogSanitizerTest {

    @Nested
    @DisplayName("sanitize")
    class Sanitize {

        @Test
        void allowsAlphanumericAndSafeChars() {
            assertThat(LogSanitizer.sanitize("hello-world_1.0 test")).isEqualTo("hello-world_1.0 test");
        }

        @Test
        void stripsControlCharacters() {
            assertThat(LogSanitizer.sanitize("line1\nline2\rline3")).isEqualTo("line1line2line3");
        }

        @Test
        void stripsSpecialCharacters() {
            assertThat(LogSanitizer.sanitize("<script>alert('xss')</script>")).isEqualTo("scriptalertxssscript");
        }

        @Test
        void returnsNullStringForNull() {
            assertThat(LogSanitizer.sanitize(null)).isEqualTo("null");
        }
    }

    @Nested
    @DisplayName("sanitizeHex")
    class SanitizeHex {

        @Test
        void allowsHexCharactersAndDots() {
            assertThat(LogSanitizer.sanitizeHex("aaBBcc00.11FF")).isEqualTo("aaBBcc00.11FF");
        }

        @Test
        void stripsNonHexCharacters() {
            assertThat(LogSanitizer.sanitizeHex("abc123\n<inject>xyz")).isEqualTo("abc123ec");
        }

        @Test
        void returnsNullStringForNull() {
            assertThat(LogSanitizer.sanitizeHex(null)).isEqualTo("null");
        }
    }

}
