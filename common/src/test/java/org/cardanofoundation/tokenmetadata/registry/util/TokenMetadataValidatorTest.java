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
    @DisplayName("Should validate token metadata with all fields within limits")
    void shouldValidateTokenMetadataWithAllFieldsWithinLimits() {
        // Given
        TokenMetadata metadata = createValidTokenMetadata();

        // When
        boolean result = validator.validate(metadata);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should validate token metadata with null optional fields")
    void shouldValidateTokenMetadataWithNullOptionalFields() {
        // Given
        TokenMetadata metadata = new TokenMetadata();
        metadata.setSubject("validSubject");
        metadata.setPolicy("validPolicy");
        metadata.setName(null);
        metadata.setTicker(null);
        metadata.setUrl(null);
        metadata.setDescription(null);
        metadata.setDecimals(null);
        metadata.setUpdated(LocalDateTime.now());
        metadata.setUpdatedBy(null);

        // When
        boolean result = validator.validate(metadata);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should validate token metadata with empty strings")
    void shouldValidateTokenMetadataWithEmptyStrings() {
        // Given
        TokenMetadata metadata = new TokenMetadata();
        metadata.setSubject("");
        metadata.setPolicy("");
        metadata.setName("");
        metadata.setTicker("");
        metadata.setUrl("");
        metadata.setDescription("");
        metadata.setUpdated(LocalDateTime.now());
        metadata.setUpdatedBy("");

        // When
        boolean result = validator.validate(metadata);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should reject token metadata when subject exceeds 255 characters")
    void shouldRejectTokenMetadataWhenSubjectExceedsMaxLength() {
        // Given
        TokenMetadata metadata = createValidTokenMetadata();
        metadata.setSubject(generateString(256));

        // When
        boolean result = validator.validate(metadata);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should accept token metadata when subject is exactly 255 characters")
    void shouldAcceptTokenMetadataWhenSubjectIsExactly255Characters() {
        // Given
        TokenMetadata metadata = createValidTokenMetadata();
        metadata.setSubject(generateString(255));

        // When
        boolean result = validator.validate(metadata);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should reject token metadata when name exceeds 255 characters")
    void shouldRejectTokenMetadataWhenNameExceedsMaxLength() {
        // Given
        TokenMetadata metadata = createValidTokenMetadata();
        metadata.setName(generateString(256));

        // When
        boolean result = validator.validate(metadata);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should accept token metadata when name is exactly 255 characters")
    void shouldAcceptTokenMetadataWhenNameIsExactly255Characters() {
        // Given
        TokenMetadata metadata = createValidTokenMetadata();
        metadata.setName(generateString(255));

        // When
        boolean result = validator.validate(metadata);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should reject token metadata when ticker exceeds 32 characters")
    void shouldRejectTokenMetadataWhenTickerExceedsMaxLength() {
        // Given
        TokenMetadata metadata = createValidTokenMetadata();
        metadata.setTicker(generateString(33));

        // When
        boolean result = validator.validate(metadata);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should accept token metadata when ticker is exactly 32 characters")
    void shouldAcceptTokenMetadataWhenTickerIsExactly32Characters() {
        // Given
        TokenMetadata metadata = createValidTokenMetadata();
        metadata.setTicker(generateString(32));

        // When
        boolean result = validator.validate(metadata);

        // Then
        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 10, 20, 32})
    @DisplayName("Should accept token metadata when ticker is within valid range")
    void shouldAcceptTokenMetadataWhenTickerIsWithinValidRange(int length) {
        // Given
        TokenMetadata metadata = createValidTokenMetadata();
        metadata.setTicker(generateString(length));

        // When
        boolean result = validator.validate(metadata);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should reject token metadata when url exceeds 255 characters")
    void shouldRejectTokenMetadataWhenUrlExceedsMaxLength() {
        // Given
        TokenMetadata metadata = createValidTokenMetadata();
        metadata.setUrl(generateString(256));

        // When
        boolean result = validator.validate(metadata);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should accept token metadata when url is exactly 255 characters")
    void shouldAcceptTokenMetadataWhenUrlIsExactly255Characters() {
        // Given
        TokenMetadata metadata = createValidTokenMetadata();
        metadata.setUrl(generateString(255));

        // When
        boolean result = validator.validate(metadata);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should reject token metadata when updatedBy exceeds 255 characters")
    void shouldRejectTokenMetadataWhenUpdatedByExceedsMaxLength() {
        // Given
        TokenMetadata metadata = createValidTokenMetadata();
        metadata.setUpdatedBy(generateString(256));

        // When
        boolean result = validator.validate(metadata);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should accept token metadata when updatedBy is exactly 255 characters")
    void shouldAcceptTokenMetadataWhenUpdatedByIsExactly255Characters() {
        // Given
        TokenMetadata metadata = createValidTokenMetadata();
        metadata.setUpdatedBy(generateString(255));

        // When
        boolean result = validator.validate(metadata);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should reject token metadata when multiple fields exceed max length")
    void shouldRejectTokenMetadataWhenMultipleFieldsExceedMaxLength() {
        // Given
        TokenMetadata metadata = createValidTokenMetadata();
        metadata.setName(generateString(256));
        metadata.setTicker(generateString(33));
        metadata.setUrl(generateString(256));

        // When
        boolean result = validator.validate(metadata);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should accept token metadata with description exceeding 255 characters (text field has no limit)")
    void shouldAcceptTokenMetadataWithLongDescription() {
        // Given
        TokenMetadata metadata = createValidTokenMetadata();
        metadata.setDescription(generateString(10000)); // description is TEXT type with no limit

        // When
        boolean result = validator.validate(metadata);

        // Then
        assertThat(result).isTrue();
    }

    // Helper methods

    private TokenMetadata createValidTokenMetadata() {
        TokenMetadata metadata = new TokenMetadata();
        metadata.setSubject("testSubject");
        metadata.setPolicy("testPolicy");
        metadata.setName("Test Token");
        metadata.setTicker("TEST");
        metadata.setUrl("https://example.com");
        metadata.setDescription("Test description");
        metadata.setDecimals(6L);
        metadata.setUpdated(LocalDateTime.now());
        metadata.setUpdatedBy("testUser");
        return metadata;
    }

    private String generateString(int length) {
        return "a".repeat(length);
    }
}
