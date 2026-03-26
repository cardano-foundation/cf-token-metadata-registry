package org.cardanofoundation.tokenmetadata.registry.it;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sanity checks for the OpenAPI (Swagger) documentation endpoint.
 * Verifies that both V1 and V2 API endpoints are documented with basic information.
 */
public class OpenApiDocsIT extends BaseIntegrationIT {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static JsonNode apiDocs;

    @BeforeAll
    static void setUp() throws Exception {
        waitForApiReady();

        ResponseEntity<String> response = restTemplate.getForEntity(API_BASE_URL + "/apidocs", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        apiDocs = objectMapper.readTree(response.getBody());
    }

    @Test
    @DisplayName("API docs should contain OpenAPI version and paths")
    void apiDocsShouldContainBasicStructure() {
        assertThat(apiDocs.has("openapi")).isTrue();
        assertThat(apiDocs.get("openapi").asText()).startsWith("3.");
        assertThat(apiDocs.has("paths")).isTrue();
        assertThat(apiDocs.get("paths").size()).isGreaterThan(0);
    }

    @Nested
    @DisplayName("V1 - Offchain metadata endpoints")
    class V1Endpoints {

        @Test
        void singleSubjectEndpointDocumented() {
            JsonNode path = apiDocs.at("/paths/~1metadata~1{subject}/get");
            assertThat(path.isMissingNode()).isFalse();
            assertThat(path.get("operationId").asText()).isEqualTo("getAllPropertiesForSubject");
            assertThat(path.has("summary")).isTrue();
        }

        @Test
        void singlePropertyEndpointDocumented() {
            JsonNode path = apiDocs.at("/paths/~1metadata~1{subject}~1properties~1{property}/get");
            assertThat(path.isMissingNode()).isFalse();
            assertThat(path.get("operationId").asText()).isEqualTo("getPropertyForSubject");
            assertThat(path.has("summary")).isTrue();
        }

        @Test
        void batchQueryEndpointDocumented() {
            JsonNode path = apiDocs.at("/paths/~1metadata~1query/post");
            assertThat(path.isMissingNode()).isFalse();
            assertThat(path.get("operationId").asText()).isEqualTo("getSubjects");
            assertThat(path.has("summary")).isTrue();
        }
    }

    @Nested
    @DisplayName("V2 - Multi-standard metadata endpoints")
    class V2Endpoints {

        @Test
        void singleSubjectEndpointDocumented() {
            JsonNode path = apiDocs.at("/paths/~1api~1v2~1subjects~1{subject}/get");
            assertThat(path.isMissingNode()).isFalse();
            assertThat(path.get("operationId").asText()).isEqualTo("getSubject");
            assertThat(path.has("summary")).isTrue();
        }

        @Test
        void batchQueryEndpointDocumented() {
            JsonNode path = apiDocs.at("/paths/~1api~1v2~1subjects~1query/post");
            assertThat(path.isMissingNode()).isFalse();
            assertThat(path.hasNonNull("operationId")).isTrue();
            assertThat(path.has("summary")).isTrue();
        }
    }

    @Nested
    @DisplayName("CIP-113 - Programmable token extensions")
    class Cip113 {

        @Test
        void subjectSchemaIncludesExtensionsField() {
            JsonNode subjectProps = apiDocs.at("/components/schemas/Subject/properties");
            assertThat(subjectProps.isMissingNode()).isFalse();
            assertThat(subjectProps.has("extensions"))
                    .as("Subject schema should have an 'extensions' field for CIP extensions")
                    .isTrue();
        }

        @Test
        void extensionsFieldIsAMapOfObjects() {
            JsonNode extensions = apiDocs.at("/components/schemas/Subject/properties/extensions");
            assertThat(extensions.isMissingNode()).isFalse();
            assertThat(extensions.path("type").asText()).isEqualTo("object");
            assertThat(extensions.has("additionalProperties"))
                    .as("extensions should be a map with additionalProperties schema")
                    .isTrue();
        }

        @Test
        void programmableTokenCip113SchemaDocumented() {
            JsonNode schema = apiDocs.at("/components/schemas/ProgrammableTokenCip113");
            assertThat(schema.isMissingNode())
                    .as("ProgrammableTokenCip113 schema should exist")
                    .isFalse();
            assertThat(schema.get("description").asText()).containsIgnoringCase("CIP-113");

            JsonNode properties = schema.path("properties");
            assertThat(properties.has("transfer_logic_script")).isTrue();
            assertThat(properties.has("third_party_transfer_logic_script")).isTrue();
            assertThat(properties.has("global_state_policy_id")).isTrue();
        }
    }

    @Nested
    @DisplayName("Response schemas")
    class Schemas {

        @Test
        void shouldIncludeKeySchemas() {
            JsonNode schemas = apiDocs.at("/components/schemas");
            assertThat(schemas.isMissingNode()).isFalse();
            assertThat(schemas.has("TokenMetadata")).isTrue();
            assertThat(schemas.has("BatchRequest")).isTrue();
        }
    }

}
