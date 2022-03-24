package org.cardanofoundation.metadatatools.core;

import org.bouncycastle.util.encoders.DecoderException;
import org.bouncycastle.util.encoders.Hex;
import org.cardanofoundation.metadatatools.core.model.TokenMetadataProperty;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class TokenMetadataValidationRules {
    private static final int MAX_NAME_LENGTH = 50;
    private static final int MAX_DESCRIPTION_LENGTH = 500;
    private static final int MIN_TICKER_LENGTH = 2;
    private static final int MAX_TICKER_LENGTH = 9;
    private static final int MIN_DECIMALS_VALUE = 0;
    private static final int POLICY_HASH_SIZE = 28;
    private static final int POLICY_HEX_STRING_LENGTH = POLICY_HASH_SIZE * 2;
    private static final List<String> REQUIRED_PROPERTIES = List.of("name", "description");

    @FunctionalInterface
    interface TokenMetadataValidatorFunction {
        ValidationResult apply(final String propertyName, final TokenMetadataProperty<?> property);
    }

    private static final Map<String, TokenMetadataValidatorFunction> VALIDATION_RULES = Map.ofEntries(
            Map.entry("name", TokenMetadataValidationRules::applyNamePropertyValidationRules),
            Map.entry("description", TokenMetadataValidationRules::applyDescriptionPropertyValidationRules),
            Map.entry("ticker", TokenMetadataValidationRules::applyTickerPropertyValidationRules),
            Map.entry("decimals", TokenMetadataValidationRules::applyDecimalsPropertyValidationRules)
    );

    private static ValidationResult applyNamePropertyValidationRules(final String propertyName, final TokenMetadataProperty<?> property) {
        final ValidationResult validationResult = applyDefaultValidationRules(propertyName, property);
        if (!(property.getValue().getClass() == String.class)) {
            validationResult.addValidationError(String.format("property %s: value is not of expected type String but %s", propertyName, property.getValue().getClass().getName()));
            return validationResult;
        }
        final String value = (String)property.getValue();
        if (value.length() > MAX_NAME_LENGTH) {
            validationResult.addValidationError(String.format("property %s: only %d characters allow but got %d", propertyName, MAX_NAME_LENGTH, value.length()));
        }
        return validationResult;
    }

    private static ValidationResult applyDescriptionPropertyValidationRules(final String propertyName, final TokenMetadataProperty<?> property) {
        final ValidationResult validationResult = applyDefaultValidationRules(propertyName, property);
        if (!(property.getValue().getClass() == String.class)) {
            validationResult.addValidationError(String.format("property %s: value is not of expected type String but %s", propertyName, property.getValue().getClass().getName()));
            return validationResult;
        }
        final String value = (String)property.getValue();
        if (value.length() > MAX_DESCRIPTION_LENGTH) {
            validationResult.addValidationError(String.format("property %s: only %d characters allow but got %d", propertyName, MAX_DESCRIPTION_LENGTH, value.length()));
        }
        return validationResult;
    }

    private static ValidationResult applyTickerPropertyValidationRules(final String propertyName, final TokenMetadataProperty<?> property) {
        final ValidationResult validationResult = applyDefaultValidationRules(propertyName, property);
        if (!(property.getValue().getClass() == String.class)) {
            validationResult.addValidationError(String.format("property %s: value is not of expected type String but %s", propertyName, property.getValue().getClass().getName()));
            return validationResult;
        }
        final String value = (String) property.getValue();
        if (value.length() < MIN_TICKER_LENGTH || value.length() > MAX_TICKER_LENGTH) {
            validationResult.addValidationError(String.format("property %s: ticker length is %d which is not in the allowed interval of [%d, %d] ", propertyName, value.length(), MIN_TICKER_LENGTH, MAX_TICKER_LENGTH));
        }
        return validationResult;
    }

    private static ValidationResult applyDecimalsPropertyValidationRules(final String propertyName, final TokenMetadataProperty<?> property) {
        final ValidationResult validationResult = applyDefaultValidationRules(propertyName, property);
        if (!(property.getValue().getClass() == Integer.class)) {
            validationResult.addValidationError(String.format("property %s: value is not of expected type Integer but %s", propertyName, property.getValue().getClass().getName()));
            return validationResult;
        }
        final int value = (int) property.getValue();
        if (value < MIN_DECIMALS_VALUE) {
            validationResult.addValidationError(String.format("property %s: value %d is not in the expected range of [%d:)", propertyName, value, MIN_DECIMALS_VALUE));
        }
        return validationResult;
    }

    private static ValidationResult applyDefaultValidationRules(final String propertyName, final TokenMetadataProperty<?> property) {
        final ValidationResult validationResult = new ValidationResult();
        if (property.getValue() == null) {
            validationResult.addValidationError(String.format("property %s: value is undefined", propertyName));
        }
        if (property.getSequenceNumber() == null) {
            validationResult.addValidationError(String.format("property %s: sequenceNumber is undefined", propertyName));
        }
        if (property.getSequenceNumber() != null && property.getSequenceNumber() < 0) {
            validationResult.addValidationError(String.format("property %s: sequenceNumber is negative (%d)", propertyName, property.getSequenceNumber()));
        }
        return validationResult;
    }

    public static ValidationResult validateProperty(final String propertyName, final TokenMetadataProperty<?> metadataProperty) {
        return VALIDATION_RULES.getOrDefault(propertyName, TokenMetadataValidationRules::applyDefaultValidationRules)
                .apply(propertyName, metadataProperty);
    }

    public static void validateHasRequiredProperties(final Set<String> propertyNames, final ValidationResult validationResult) {
        if (!propertyNames.containsAll(REQUIRED_PROPERTIES)) {
            validationResult.addValidationError(String.format("Missing required properties. Required properties are %s", REQUIRED_PROPERTIES));
        }
    }

    public static void validateSubjectAndPolicy(final String subject, final String policy, final ValidationResult validationResult) {
        if (subject == null || subject.isEmpty() || subject.isBlank()) {
            validationResult.addValidationError("Missing, empty or blank subject.");
        }

        if (subject != null && policy != null) {
            try {
                Hex.decode(policy);
            } catch (final DecoderException e) {
                validationResult.addValidationError(String.format("Cannot decode hex string representation of policy hash due to %s", e.getMessage()));
            }

            try {
                Hex.decode(subject);
            } catch (final DecoderException e) {
                validationResult.addValidationError(String.format("Cannot decode hex string representation of subject hash due to %s", e.getMessage()));
            }

            if (!subject.startsWith(policy)) {
                validationResult.addValidationError("If a policy is given the first 28 bytes of the subject should match the policy.");
            }
        }
    }
}
