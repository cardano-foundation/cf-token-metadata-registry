package org.cardanofoundation.tokenmetadata.registry.validation;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The shape of a validation error on the wire and in the JSON column: snake_case names, and a {@code sub_standard} only
 * where there is one. The responses that carry them are tested in the api module.
 */
class ValidationErrorJsonTest {

    private final ObjectMapper mapper = new JsonMapper();

    private static final ValidationError ERROR = ValidationError.mandatory("CIP-68", "333", "description", "missing_value",
            "description field is missing a mandatory value");

    @Test
    void aValidationErrorIsWrittenInSnakeCase() throws Exception {
        JsonNode json = mapper.readTree(mapper.writeValueAsString(ERROR));

        assertThat(json.get("cip").asText()).isEqualTo("CIP-68");
        assertThat(json.get("sub_standard").asText()).isEqualTo("333");
        assertThat(json.get("field_name").asText()).isEqualTo("description");
        assertThat(json.get("field_category").asText()).isEqualTo("MANDATORY");
        assertThat(json.get("code").asText()).isEqualTo("missing_value");
        assertThat(json.get("error").asText()).isEqualTo("description field is missing a mandatory value");
        assertThat(json.propertyNames()).doesNotContain("subStandard", "fieldName", "fieldCategory");
    }

    @Test
    void theSubStandardIsLeftOutWhenThereIsNone() throws Exception {
        ValidationError cip26 = ValidationError.optional("CIP-26", null, "ticker", "invalid_length", "ticker is too long");

        assertThat(mapper.readTree(mapper.writeValueAsString(cip26)).has("sub_standard")).isFalse();
    }

    @Test
    void aListOfErrorsSurvivesTheRoundTripThroughTheJsonColumn() throws Exception {
        String json = mapper.writeValueAsString(List.of(ERROR));

        List<ValidationError> back = mapper.readValue(json, new TypeReference<>() { });

        assertThat(back).containsExactly(ERROR);
    }
}
