package org.cardanofoundation.tokenmetadata.registry.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.JsonSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Configuration
public class OpenApiConfig {

    /**
     * Publishes a nullable field that refers to another schema as {@code anyOf: [{$ref}, {type: null}]}.
     * <p>
     * In OpenAPI 3.1, swagger-core writes such a field as {@code {"type": "null", "$ref": ...}}. The keywords next to a
     * {@code $ref} apply together with it, so that reads as "only null" and rejects the object the API sends. The API
     * writes these fields as null when they have no value ({@code metadata.decimals} of a token without decimals,
     * {@code standards} without {@code show_cips_details}), so the schema has to allow both.
     */
    @Bean
    public OpenApiCustomizer nullableReferencesCustomizer() {
        return OpenApiConfig::publishNullableReferences;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static void publishNullableReferences(OpenAPI openApi) {
        if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
            return;
        }
        for (Schema<?> schema : openApi.getComponents().getSchemas().values()) {
            Map<String, Schema> properties = schema.getProperties();
            if (properties != null) {
                properties.replaceAll((name, property) -> isNullableReference(property) ? referenceOrNull(property) : property);
            }
        }
    }

    private static boolean isNullableReference(Schema<?> property) {
        return property.get$ref() != null
                && (Boolean.TRUE.equals(property.getNullable())
                || property.getTypes() != null && property.getTypes().contains("null"));
    }

    private static Schema<?> referenceOrNull(Schema<?> property) {
        Schema<?> nullable = new JsonSchema();
        nullable.setAnyOf(List.of(new JsonSchema().$ref(property.get$ref()), new JsonSchema().types(Set.of("null"))));
        nullable.setDescription(property.getDescription());
        return nullable;
    }

}
