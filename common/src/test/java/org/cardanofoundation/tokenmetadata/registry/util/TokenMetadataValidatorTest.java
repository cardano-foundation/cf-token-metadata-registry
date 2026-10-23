package org.cardanofoundation.tokenmetadata.registry.util;

import org.cardanofoundation.tokenmetadata.registry.model.Item;
import org.cardanofoundation.tokenmetadata.registry.model.Mapping;
import org.cardanofoundation.tokenmetadata.registry.entity.TokenMetadata;
import org.cardanofoundation.tokenmetadata.registry.validation.FieldCategory;
import org.cardanofoundation.tokenmetadata.registry.validation.ValidationError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CIP-26 entries that break the specification are stored and served with {@link ValidationError}s (ADR-019);
 * only a subject that cannot be a key, or a logo far beyond any real size, is dropped.
 */
@SuppressWarnings("java:S2187") // tests are in @Nested inner classes
@DisplayName("TokenMetadataValidator")
class TokenMetadataValidatorTest {

    // Valid 56-char hex subject (28-byte policy ID + short asset name)
    private static final String VALID_SUBJECT = "025146866af908340247fe4e9672d5ac7059f1e8534696b5f920c9e66362544848";

    private final TokenMetadataValidator validator = new TokenMetadataValidator();

    private static TokenMetadata metadata(String subject, String name, String description) {
        TokenMetadata m = new TokenMetadata();
        m.setSubject(subject);
        m.setName(name);
        m.setDescription(description);
        return m;
    }

    private Cip26Validation validate(TokenMetadata m) {
        return validator.validate(null, m);
    }

    private static List<String> summary(Cip26Validation validation) {
        return validation.errors().stream()
                .map(e -> e.fieldName() + ":" + e.fieldCategory() + ":" + e.code()).toList();
    }

    @Nested
    @DisplayName("validate — an entry that follows CIP-26")
    class ValidMetadata {

        @Test
        void hasNoErrorsForMinimalValidMetadata() {
            Cip26Validation result = validate(metadata(VALID_SUBJECT, "Nutcoin", "A test token"));

            assertThat(result.isDropped()).isFalse();
            assertThat(result.errors()).isEmpty();
        }

        @Test
        void hasNoErrorsWithAllOptionalFields() {
            TokenMetadata m = metadata(VALID_SUBJECT, "Nutcoin", "A test token");
            m.setTicker("NUT");
            m.setUrl("https://example.com");
            m.setDecimals(6L);
            assertThat(validate(m).errors()).isEmpty();
        }

        @Test
        void acceptsTheBoundsOfDecimalsAndDescription() {
            TokenMetadata zero = metadata(VALID_SUBJECT, "Token", "Description");
            zero.setDecimals(0L);
            TokenMetadata max = metadata(VALID_SUBJECT, "Token", "d".repeat(500));
            max.setDecimals(255L);

            assertThat(validate(zero).errors()).isEmpty();
            assertThat(validate(max).errors()).isEmpty();
        }
    }

    @Nested
    @DisplayName("validate — an entry that breaks CIP-26 is kept, with errors")
    class InvalidMetadata {

        @Test
        void reportsAMissingNameAsMandatory() {
            Cip26Validation result = validate(metadata(VALID_SUBJECT, null, "A description"));

            assertThat(result.isDropped()).isFalse();
            assertThat(result.errors()).singleElement().satisfies(e -> {
                assertThat(e.cip()).isEqualTo("CIP-26");
                assertThat(e.subStandard()).isNull();
                assertThat(e.fieldName()).isEqualTo("name");
                assertThat(e.fieldCategory()).isEqualTo(FieldCategory.MANDATORY);
                assertThat(e.code()).isEqualTo("missing_value");
                assertThat(e.error()).isEqualTo("name field is missing a mandatory value");
            });
        }

        @Test
        void reportsAMissingDescriptionAsMandatory() {
            assertThat(summary(validate(metadata(VALID_SUBJECT, "Nutcoin", null))))
                    .containsExactly("description:MANDATORY:missing_value");
        }

        @Test
        void reportsBothWhenNameAndDescriptionAreMissing() {
            assertThat(summary(validate(metadata(VALID_SUBJECT, null, null))))
                    .containsExactly("name:MANDATORY:missing_value", "description:MANDATORY:missing_value");
        }

        @Test
        void reportsANameOverTheLimitAsAMandatoryFieldWithALengthError() {
            Cip26Validation result = validate(metadata(VALID_SUBJECT, "A".repeat(100), "A description"));

            assertThat(result.errors()).singleElement().satisfies(e -> {
                assertThat(e.fieldName()).isEqualTo("name");
                assertThat(e.fieldCategory()).isEqualTo(FieldCategory.MANDATORY);
                assertThat(e.code()).isEqualTo("invalid_length");
                assertThat(e.error()).contains("50").contains("100");
            });
        }

        @Test
        void reportsATickerOverTheLimitAsOptional() {
            TokenMetadata m = metadata(VALID_SUBJECT, "Token", "Description");
            m.setTicker("ABCDEFGHIJ"); // CIP-26: ticker max 9 chars
            assertThat(summary(validate(m))).containsExactly("ticker:OPTIONAL:invalid_length");
        }

        @Test
        void reportsAUrlOverTheLimitAsOptional() {
            TokenMetadata m = metadata(VALID_SUBJECT, "Token", "Description");
            m.setUrl("https://example.com/" + "a".repeat(300)); // CIP-26: url max 250 chars
            assertThat(summary(validate(m))).containsExactly("url:OPTIONAL:invalid_length");
        }

        @Test
        void reportsADescriptionOverTheLimit() {
            assertThat(summary(validate(metadata(VALID_SUBJECT, "Token", "d".repeat(501)))))
                    .containsExactly("description:MANDATORY:invalid_length");
        }

        @Test
        void reportsDecimalsOutsideTheRangeAndKeepsTheValue() {
            for (long decimals : new long[]{-1L, 256L, 4_294_967_301L}) {
                // 2^32 + 5: narrowing to int gives 5, which the upstream library would accept
                TokenMetadata m = metadata(VALID_SUBJECT, "Token", "Description");
                m.setDecimals(decimals);

                Cip26Validation result = validate(m);

                assertThat(summary(result)).as("decimals %d", decimals).containsExactly("decimals:OPTIONAL:out_of_range");
                assertThat(m.getDecimals()).isEqualTo(decimals);
            }
        }

        @Test
        void reportsDecimalsThatAreNotANumber() {
            // MappingsUtil turns a non-numeric value into null (and the raw string stays in the mapping)
            TokenMetadata m = metadata(VALID_SUBJECT, "Token", "Description");
            Mapping mapping = new Mapping(VALID_SUBJECT, null, null, null, new Item(1, "six", null), null, null, null);

            Cip26Validation result = validator.validate(mapping, m);

            assertThat(result.errors()).singleElement().satisfies(e -> {
                assertThat(e.fieldName()).isEqualTo("decimals");
                assertThat(e.code()).isEqualTo("wrong_type");
                assertThat(e.error()).contains("'six'");
            });
        }

        @Test
        void reportsEveryProblemOfAnEntryAtOnce() {
            TokenMetadata m = metadata(VALID_SUBJECT, null, "d".repeat(501));
            m.setTicker("A");
            m.setDecimals(300L);

            assertThat(summary(validate(m))).containsExactlyInAnyOrder(
                    "name:MANDATORY:missing_value", "decimals:OPTIONAL:out_of_range",
                    "description:MANDATORY:invalid_length", "ticker:OPTIONAL:invalid_length");
        }
    }

    @Nested
    @DisplayName("validate — a subject that cannot be a key drops the entry")
    class Subject {

        @Test
        void dropsAMissingSubject() {
            Cip26Validation result = validate(metadata(null, "Nutcoin", "A description"));

            assertThat(result.isDropped()).isTrue();
            assertThat(result.droppedReason()).contains("missing");
        }

        @Test
        void dropsASubjectThatIsNotEvenLengthHex() {
            assertThat(validate(metadata(VALID_SUBJECT + "0", "N", "D")).isDropped()).isTrue();
            assertThat(validate(metadata("zz" + VALID_SUBJECT.substring(2), "N", "D")).isDropped()).isTrue();
        }

        @Test
        void dropsASubjectShorterThanAPolicyIdOrLongerThanTheMaximum() {
            assertThat(validate(metadata("0251", "N", "D")).isDropped()).isTrue();
            assertThat(validate(metadata("02".repeat(61), "N", "D")).isDropped()).isTrue();
        }

        @Test
        void keepsASubjectAtTheBounds() {
            assertThat(validate(metadata("02".repeat(28), "N", "D")).isDropped()).isFalse();
            assertThat(validate(metadata("02".repeat(60), "N", "D")).isDropped()).isFalse();
        }
    }

    @Nested
    @DisplayName("validateLogo")
    class ValidateLogo {

        @Test
        void hasNoErrorsForANullOrEmptyLogo() {
            assertThat(validator.validateLogo(VALID_SUBJECT, null).errors()).isEmpty();
            assertThat(validator.validateLogo(VALID_SUBJECT, "").errors()).isEmpty();
        }

        @Test
        void hasNoErrorsForAValidBase64PngLogo() {
            // Minimal valid PNG as base64 (1x1 transparent pixel)
            String validPng = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";
            Cip26Validation result = validator.validateLogo(VALID_SUBJECT, validPng);

            assertThat(result.isDropped()).isFalse();
            assertThat(result.errors()).isEmpty();
        }

        @Test
        void reportsALogoOverTheCipLimitButKeepsIt() {
            // CIP-26: logo max 87400 base64 chars (64 KiB decoded)
            Cip26Validation result = validator.validateLogo(VALID_SUBJECT, "A".repeat(90000));

            assertThat(result.isDropped()).isFalse();
            assertThat(result.errors()).singleElement().satisfies(e -> {
                assertThat(e.fieldName()).isEqualTo("logo");
                assertThat(e.fieldCategory()).isEqualTo(FieldCategory.OPTIONAL);
                assertThat(e.code()).isEqualTo("too_large");
            });
        }

        @Test
        void reportsNonBase64GarbageButKeepsIt() {
            // Characters outside the base64 alphabet: Base64.getDecoder() throws, which becomes an error
            Cip26Validation result = validator.validateLogo(VALID_SUBJECT, "this-is-not!!base64@@");

            assertThat(result.isDropped()).isFalse();
            assertThat(summary(result)).containsExactly("logo:OPTIONAL:invalid_base64");
        }

        @Test
        void dropsALogoFarBeyondAnyRealSize() {
            // an obvious attack on the indexer: far beyond the 64 KiB CIP-26 allows
            Cip26Validation result = validator.validateLogo(VALID_SUBJECT, "A".repeat(TokenMetadataValidator.LOGO_HARD_CAP_CHARS + 4));

            assertThat(result.isDropped()).isTrue();
        }

        @Test
        void handlesAnInvalidSubjectGracefully() {
            assertThat(validator.validateLogo("invalid", null).errors()).isEmpty();
        }
    }
}
