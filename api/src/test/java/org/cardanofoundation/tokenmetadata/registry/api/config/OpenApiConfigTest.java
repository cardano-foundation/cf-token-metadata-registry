package org.cardanofoundation.tokenmetadata.registry.api.config;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.util.Json31;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import org.cardanofoundation.tokenmetadata.registry.api.model.v2.Subject;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The V2 schemas as springdoc publishes them (OpenAPI 3.1), rendered from the model classes by swagger-core with the
 * customizer applied: a field the API writes as null must accept null, and a field it writes as an object must accept
 * the object.
 */
class OpenApiConfigTest {

    private final JsonNode schemas = renderSchemas();

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static JsonNode renderSchemas() {
        Map<String, Schema> rendered = ModelConverters.getInstance(true).readAll(Subject.class);
        OpenAPI openApi = new OpenAPI().components(new Components().schemas(rendered));
        OpenApiConfig.publishNullableReferences(openApi);
        return Json31.mapper().valueToTree(openApi).path("components").path("schemas");
    }

    @Nested
    class NullableReferences {

        @ParameterizedTest
        @CsvSource({
                "Subject, standards, Standards",
                "Metadata, name, StringProperty",
                "Metadata, description, StringProperty",
                "Metadata, ticker, StringProperty",
                "Metadata, decimals, LongProperty",
                "Metadata, logo, StringProperty",
                "Metadata, url, StringProperty",
                "Metadata, version, LongProperty",
                "Standards, cip26, TokenMetadata",
                "Standards, cip68, FungibleTokenMetadata"
        })
        void areTheReferenceOrNull(String schema, String property, String reference) {
            JsonNode field = schemas.path(schema).path("properties").path(property);

            assertThat(field.has("$ref")).as("no $ref next to anyOf").isFalse();
            assertThat(field.has("type")).as("no type next to anyOf").isFalse();
            assertThat(field.path("anyOf"))
                    .hasToString("[{\"$ref\":\"#/components/schemas/" + reference + "\"},{\"type\":\"null\"}]");
        }

        @Test
        void keepTheirDescription() {
            assertThat(schemas.path("Subject").path("properties").path("standards").path("description").asText())
                    .isEqualTo("Raw per-standard metadata when show_cips_details=true, null otherwise");
        }
    }

    @Nested
    class OtherFields {

        @Test
        void metadataIsAPlainReference() {
            // never null: a subject always has its metadata block
            assertThat(schemas.path("Subject").path("properties").path("metadata").path("$ref").asText())
                    .isEqualTo("#/components/schemas/Metadata");
        }

        @Test
        void extensionsIsAMapOfObjectsThatIsNeverNull() {
            // left out of the response when empty (@JsonInclude NON_EMPTY), never written as null
            JsonNode extensions = schemas.path("Subject").path("properties").path("extensions");

            assertThat(extensions.path("type").asText()).isEqualTo("object");
            assertThat(extensions.has("additionalProperties")).isTrue();
        }

        @Test
        void metadataHasNoEmptyProperty() {
            // Metadata.isEmpty() is a helper (@JsonIgnore), not a property of the response
            JsonNode properties = schemas.path("Metadata").path("properties");

            assertThat(properties.has("valid")).isTrue();
            assertThat(properties.has("empty")).isFalse();
        }

        @Test
        void nullableStringsStayATypeList() {
            for (String field : List.of("name", "description", "ticker", "logo", "url")) {
                JsonNode type = schemas.path("FungibleTokenMetadata").path("properties").path(field).path("type");
                assertThat(type).as(field).hasToString("[\"string\",\"null\"]");
            }
        }
    }

}
