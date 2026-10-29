package org.cardanofoundation.tokenmetadata.registry.api.model.v2;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip68.FungibleTokenMetadata;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.TokenMetadata;
import org.cardanofoundation.tokenmetadata.registry.validation.ValidationError;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The shape of {@code validation_errors} in the V2 responses (ADR-019), with the API's mapper settings (null properties
 * left out): snake_case, an array that is there when the token was checked (empty when it follows its CIP), and absent
 * for a token indexed before validation existed. The V1 response, which shares the CIP-26 model, never carries it.
 */
class ValidationErrorsJsonTest {

    private final ObjectMapper mapper = new ObjectMapper().setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);

    private static final ValidationError ERROR = ValidationError.mandatory("CIP-68", "333", "description", "missing_value",
            "description field is missing a mandatory value");

    @Test
    void aSubjectHasAValidationErrorsArrayWhenTheTokenWasChecked() throws Exception {
        JsonNode with = json(new Subject("s", TokenType.NATIVE, Metadata.empty(), null, null, List.of(ERROR)));
        JsonNode none = json(new Subject("s", TokenType.NATIVE, Metadata.empty(), null, null, List.of()));
        JsonNode unchecked = json(new Subject("s", TokenType.NATIVE, Metadata.empty(), null, null, null));

        assertThat(with.get("validation_errors")).hasSize(1);
        assertThat(with.get("validation_errors").get(0).get("code").asText()).isEqualTo("missing_value");
        assertThat(none.get("validation_errors").isArray()).isTrue();
        assertThat(none.get("validation_errors")).isEmpty();
        assertThat(unchecked.has("validation_errors")).isFalse();
    }

    @Test
    void theMetadataKeepsItsValidAndEmptyFlags() throws Exception {
        StringProperty name = new StringProperty("n", "CIP_68");

        assertThat(json(Metadata.builder().name(name).valid(true).build()).get("valid").asBoolean()).isTrue();
        assertThat(json(Metadata.builder().name(name).build()).get("valid").asBoolean()).isFalse();
        assertThat(json(Metadata.empty()).get("empty").asBoolean()).isTrue();
    }

    @Test
    void theCip68StandardWritesValidationErrorsInSnakeCase() throws Exception {
        JsonNode cip68 = json(new FungibleTokenMetadata(null, null, null, "N", null, null, 1L, List.of(ERROR)));

        assertThat(cip68.get("validation_errors")).hasSize(1);
        assertThat(cip68.has("validationErrors")).isFalse();
    }

    @Test
    void theCip26StandardCarriesValidationErrorsOnlyWhenSet() throws Exception {
        TokenMetadata v2 = TokenMetadata.builder().subject("s").validationErrors(List.of()).build();
        TokenMetadata v1 = TokenMetadata.builder().subject("s").build();

        assertThat(json(v2).get("validation_errors").isArray()).isTrue();
        assertThat(json(v1).has("validation_errors")).isFalse();
    }

    private JsonNode json(Object value) throws Exception {
        return mapper.readTree(mapper.writeValueAsString(value));
    }
}
