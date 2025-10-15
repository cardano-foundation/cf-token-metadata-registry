package org.cardanofoundation.tokenmetadata.registry.util;

import org.cardanofoundation.tokenmetadata.registry.entity.TokenMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TokenMetadataValidatorTest {

    private TokenMetadataValidator validator;

    @BeforeEach
    void setUp() {
        validator = new TokenMetadataValidator();
    }

    @Test
    @DisplayName("Should validate token metadata with all required CIP-26 fields")
    void shouldValidateTokenMetadataWithAllFieldsWithinLimits() {
        // Given
        TokenMetadata metadata = createValidTokenMetadata();

        // When
        boolean result = validator.validate(metadata);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should reject token metadata with missing required fields (name and description)")
    void shouldValidateTokenMetadataWithNullOptionalFields() {
        // Given - CIP-26 requires name and description
        TokenMetadata metadata = new TokenMetadata();
        metadata.setSubject("1234567890abcdef1234567890abcdef1234567890abcdef1234567890"); // 58 hex chars (valid subject)
        metadata.setPolicy("validPolicy");
        metadata.setName(null); // Required in CIP-26
        metadata.setTicker(null);
        metadata.setUrl(null);
        metadata.setDescription(null); // Required in CIP-26
        metadata.setDecimals(null);
        metadata.setUpdated(LocalDateTime.now());
        metadata.setUpdatedBy(null);

        // When
        boolean result = validator.validate(metadata);

        // Then - Should fail because name and description are required
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should reject token metadata with empty required fields")
    void shouldValidateTokenMetadataWithEmptyStrings() {
        // Given - CIP-26 requires non-empty name and description, and valid subject
        TokenMetadata metadata = new TokenMetadata();
        metadata.setSubject(""); // Invalid - subject cannot be empty
        metadata.setPolicy("");
        metadata.setName(""); // Invalid - required field
        metadata.setTicker("");
        metadata.setUrl("");
        metadata.setDescription(""); // Invalid - required field
        metadata.setUpdated(LocalDateTime.now());
        metadata.setUpdatedBy("");

        // When
        boolean result = validator.validate(metadata);

        // Then - Should fail due to empty required fields
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should reject token metadata when subject exceeds 120 hex characters")
    void shouldRejectTokenMetadataWhenSubjectExceedsMaxLength() {
        // Given - Subject must be 56-120 hex characters in CIP-26
        TokenMetadata metadata = createValidTokenMetadata();
        metadata.setSubject(generateHexString(122)); // 122 hex chars, exceeds max

        // When
        boolean result = validator.validate(metadata);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should accept token metadata when subject is exactly 120 hex characters")
    void shouldAcceptTokenMetadataWhenSubjectIsExactly120Characters() {
        // Given - 120 hex characters is the maximum allowed for subject
        TokenMetadata metadata = createValidTokenMetadata();
        metadata.setSubject(generateHexString(120));

        // When
        boolean result = validator.validate(metadata);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should reject token metadata when name exceeds 50 characters (CIP-26 limit)")
    void shouldRejectTokenMetadataWhenNameExceedsMaxLength() {
        // Given - CIP-26 limits name to 50 characters
        TokenMetadata metadata = createValidTokenMetadata();
        metadata.setName(generateString(51));

        // When
        boolean result = validator.validate(metadata);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should accept token metadata when name is exactly 50 characters (CIP-26 limit)")
    void shouldAcceptTokenMetadataWhenNameIsExactly255Characters() {
        // Given - CIP-26 allows up to 50 characters for name
        TokenMetadata metadata = createValidTokenMetadata();
        metadata.setName(generateString(50));

        // When
        boolean result = validator.validate(metadata);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should reject token metadata when ticker exceeds 9 characters (CIP-26 limit)")
    void shouldRejectTokenMetadataWhenTickerExceedsMaxLength() {
        // Given - CIP-26 limits ticker to 2-9 characters
        TokenMetadata metadata = createValidTokenMetadata();
        metadata.setTicker(generateString(10));

        // When
        boolean result = validator.validate(metadata);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should accept token metadata when ticker is exactly 9 characters (CIP-26 limit)")
    void shouldAcceptTokenMetadataWhenTickerIsExactly32Characters() {
        // Given - CIP-26 allows up to 9 characters for ticker
        TokenMetadata metadata = createValidTokenMetadata();
        metadata.setTicker(generateString(9));

        // When
        boolean result = validator.validate(metadata);

        // Then
        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 5, 9})
    @DisplayName("Should accept token metadata when ticker is within valid CIP-26 range (2-9 chars)")
    void shouldAcceptTokenMetadataWhenTickerIsWithinValidRange(int length) {
        // Given - CIP-26 requires ticker to be 2-9 characters
        TokenMetadata metadata = createValidTokenMetadata();
        metadata.setTicker(generateString(length));

        // When
        boolean result = validator.validate(metadata);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should reject token metadata when ticker is only 1 character (below CIP-26 minimum)")
    void shouldRejectTokenMetadataWhenTickerIsTooShort() {
        // Given - CIP-26 requires ticker to be at least 2 characters
        TokenMetadata metadata = createValidTokenMetadata();
        metadata.setTicker("A");

        // When
        boolean result = validator.validate(metadata);

        // Then
        assertThat(result).isFalse();
    }


    @Test
    @DisplayName("Should reject token metadata when description exceeds 500 characters (CIP-26 limit)")
    void shouldRejectTokenMetadataWhenDescriptionExceedsMaxLength() {
        // Given - CIP-26 limits description to 500 characters
        TokenMetadata metadata = createValidTokenMetadata();
        metadata.setDescription(generateString(501));

        // When
        boolean result = validator.validate(metadata);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should accept token metadata when description is exactly 500 characters (CIP-26 limit)")
    void shouldAcceptTokenMetadataWhenDescriptionIsExactly500Characters() {
        // Given - CIP-26 allows up to 500 characters for description
        TokenMetadata metadata = createValidTokenMetadata();
        metadata.setDescription(generateString(500));

        // When
        boolean result = validator.validate(metadata);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should reject token metadata when multiple fields exceed CIP-26 limits")
    void shouldRejectTokenMetadataWhenMultipleFieldsExceedMaxLength() {
        // Given - Multiple fields violating CIP-26 constraints
        TokenMetadata metadata = createValidTokenMetadata();
        metadata.setName(generateString(51)); // Max 50
        metadata.setTicker(generateString(10)); // Max 9
        metadata.setDescription(generateString(501)); // Max 500

        // When
        boolean result = validator.validate(metadata);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should accept token metadata with null optional fields (ticker, decimals, url)")
    void shouldAcceptTokenMetadataWithNullOptionalFields() {
        // Given - Only required fields (subject, name, description) are provided
        TokenMetadata metadata = new TokenMetadata();
        metadata.setSubject(generateHexString(56)); // Valid 56 hex char subject
        metadata.setPolicy("testPolicy");
        metadata.setName("Test Token");
        metadata.setTicker(null); // Optional
        metadata.setUrl(null); // Optional
        metadata.setDescription("Test description");
        metadata.setDecimals(null); // Optional
        metadata.setUpdated(LocalDateTime.now());
        metadata.setUpdatedBy("testUser");

        // When
        boolean result = validator.validate(metadata);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should reject token metadata with negative decimals")
    void shouldRejectTokenMetadataWithNegativeDecimals() {
        // Given - CIP-26 requires non-negative decimals
        TokenMetadata metadata = createValidTokenMetadata();
        metadata.setDecimals(-1L);

        // When
        boolean result = validator.validate(metadata);

        // Then
        assertThat(result).isFalse();
    }

    // Logo validation tests

    @Test
    @DisplayName("Should accept valid logo URL")
    void shouldAcceptValidLogoUrl() {
        // Given - Valid logo URL
        String subject = generateHexString(56);
        String logo = "https://example.com/logo.png";

        // When
        boolean result = validator.validateLogo(subject, logo);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should accept logo with data URI")
    void shouldAcceptLogoWithDataUri() {
        // Given - Logo as data URI
        String subject = generateHexString(56);
        String logo = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";

        // When
        boolean result = validator.validateLogo(subject, logo);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should accept null logo (optional field)")
    void shouldAcceptNullLogo() {
        // Given - Logo is optional
        String subject = generateHexString(56);
        String logo = null;

        // When
        boolean result = validator.validateLogo(subject, logo);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should accept empty logo string (optional field)")
    void shouldAcceptEmptyLogo() {
        // Given - Logo is optional
        String subject = generateHexString(56);
        String logo = "";

        // When
        boolean result = validator.validateLogo(subject, logo);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should accept logo at maximum length (87400 characters per CIP-26)")
    void shouldAcceptLogoAtMaximumLength() {
        // Given - CIP-26 specification allows up to 87,400 characters for logo
        // This limit is enforced by cf-metadata-core library
        String subject = generateHexString(56);
        String logo = generateString(87400);

        // When
        boolean result = validator.validateLogo(subject, logo);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should reject logo exceeding maximum length (87400 characters per CIP-26)")
    void shouldRejectLogoExceedingMaximumLength() {
        // Given - Logo exceeds CIP-26 specification limit (87,400 chars)
        // Validation is performed by cf-metadata-core library
        String subject = generateHexString(56);
        String logo = generateString(87401);

        // When
        boolean result = validator.validateLogo(subject, logo);

        // Then
        assertThat(result).isFalse();
    }

    // Helper methods

    private TokenMetadata createValidTokenMetadata() {
        TokenMetadata metadata = new TokenMetadata();
        // Valid 56 hex character subject (policy ID only, minimum length)
        metadata.setSubject("1234567890abcdef1234567890abcdef1234567890abcdef12345678");
        metadata.setPolicy("testPolicy");
        metadata.setName("Test Token"); // Within 50 char limit
        metadata.setTicker("TEST"); // 2-9 characters
        metadata.setUrl("https://example.com");
        metadata.setDescription("Test description"); // Within 500 char limit
        metadata.setDecimals(6L);
        metadata.setUpdated(LocalDateTime.now());
        metadata.setUpdatedBy("testUser");
        return metadata;
    }

    private String generateString(int length) {
        return "a".repeat(length);
    }

    private String generateHexString(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(Integer.toHexString(i % 16));
        }
        return sb.toString();
    }
}
