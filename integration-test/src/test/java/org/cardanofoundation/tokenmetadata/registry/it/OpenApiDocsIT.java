package org.cardanofoundation.tokenmetadata.registry.it;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Iterator;
import java.util.Map;
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
        void programmableTokenSchemaDocumented() {
            JsonNode schemas = apiDocs.at("/components/schemas");
            // Springdoc may name it ProgrammableTokenCip113 or inline it — find by scanning all schemas
            boolean found = false;
            Iterator<Map.Entry<String, JsonNode>> it = schemas.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> entry = it.next();
                JsonNode props = entry.getValue().path("properties");
                if (props.has("transfer_logic_script")
                        && props.has("third_party_transfer_logic_script")
                        && props.has("global_state_policy_id")) {
                    found = true;
                    break;
                }
            }
            assertThat(found)
                    .as("A schema with CIP-113 fields (transfer_logic_script, third_party_transfer_logic_script, global_state_policy_id) should exist")
                    .isTrue();
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
