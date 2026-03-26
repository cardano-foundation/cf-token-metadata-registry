package org.cardanofoundation.tokenmetadata.registry.it;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sanity checks for the OpenAPI (Swagger) documentation endpoint.
 * Verifies that both V1 and V2 API endpoints are documented with basic information.
 */
public class OpenApiDocsIT extends BaseIntegrationIT {

    private static DocumentContext apiDocs;

    @BeforeAll
    static void setUp() {
        waitForApiReady();

        ResponseEntity<String> response = restTemplate.getForEntity(API_BASE_URL + "/apidocs", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        apiDocs = JsonPath.parse(response.getBody());
    }

    @Test
    @DisplayName("API docs should contain OpenAPI version and paths")
    void apiDocsShouldContainBasicStructure() {
        assertThat(apiDocs.read("$.openapi", String.class)).startsWith("3.");
        Map<String, Object> paths = apiDocs.read("$.paths", Map.class);
        assertThat(paths).isNotEmpty();
    }

    @Nested
    @DisplayName("V1 - Offchain metadata endpoints")
    class V1Endpoints {

        @Test
        void singleSubjectEndpointDocumented() {
            Object path = apiDocs.read("$.paths['/metadata/{subject}'].get", Object.class);
            assertThat(path).isNotNull();
            assertThat(apiDocs.read("$.paths['/metadata/{subject}'].get.operationId", String.class))
                    .isEqualTo("getAllPropertiesForSubject");
            assertThat(apiDocs.read("$.paths['/metadata/{subject}'].get.summary", String.class)).isNotNull();
        }

        @Test
        void singlePropertyEndpointDocumented() {
            Object path = apiDocs.read("$.paths['/metadata/{subject}/properties/{property}'].get", Object.class);
            assertThat(path).isNotNull();
            assertThat(apiDocs.read("$.paths['/metadata/{subject}/properties/{property}'].get.operationId", String.class))
                    .isEqualTo("getPropertyForSubject");
            assertThat(apiDocs.read("$.paths['/metadata/{subject}/properties/{property}'].get.summary", String.class)).isNotNull();
        }

        @Test
        void batchQueryEndpointDocumented() {
            Object path = apiDocs.read("$.paths['/metadata/query'].post", Object.class);
            assertThat(path).isNotNull();
            assertThat(apiDocs.read("$.paths['/metadata/query'].post.operationId", String.class))
                    .isEqualTo("getSubjects");
            assertThat(apiDocs.read("$.paths['/metadata/query'].post.summary", String.class)).isNotNull();
        }
    }

    @Nested
    @DisplayName("V2 - Multi-standard metadata endpoints")
    class V2Endpoints {

        @Test
        void singleSubjectEndpointDocumented() {
            Object path = apiDocs.read("$.paths['/api/v2/subjects/{subject}'].get", Object.class);
            assertThat(path).isNotNull();
            assertThat(apiDocs.read("$.paths['/api/v2/subjects/{subject}'].get.operationId", String.class))
                    .isEqualTo("getSubject");
            assertThat(apiDocs.read("$.paths['/api/v2/subjects/{subject}'].get.summary", String.class)).isNotNull();
        }

        @Test
        void batchQueryEndpointDocumented() {
            Object path = apiDocs.read("$.paths['/api/v2/subjects/query'].post", Object.class);
            assertThat(path).isNotNull();
            assertThat(apiDocs.read("$.paths['/api/v2/subjects/query'].post.operationId", String.class)).isNotNull();
            assertThat(apiDocs.read("$.paths['/api/v2/subjects/query'].post.summary", String.class)).isNotNull();
        }
    }

    @Nested
    @DisplayName("CIP-113 - Programmable token extensions")
    class Cip113 {

        @Test
        void subjectSchemaIncludesExtensionsField() {
            assertThat(apiDocs.read("$.components.schemas.Subject.properties.extensions", Object.class))
                    .as("Subject schema should have an 'extensions' field for CIP extensions")
                    .isNotNull();
        }

        @Test
        void extensionsFieldIsAMapOfObjects() {
            assertThat(apiDocs.read("$.components.schemas.Subject.properties.extensions.type", String.class))
                    .isEqualTo("object");
            assertThat(apiDocs.read("$.components.schemas.Subject.properties.extensions.additionalProperties", Object.class))
                    .as("extensions should be a map with additionalProperties schema")
                    .isNotNull();
        }

        @Test
        void programmableTokenCip113SchemaDocumented() {
            assertThat(apiDocs.read("$.components.schemas.ProgrammableTokenCip113", Object.class))
                    .as("ProgrammableTokenCip113 schema should exist")
                    .isNotNull();
            assertThat(apiDocs.read("$.components.schemas.ProgrammableTokenCip113.description", String.class))
                    .containsIgnoringCase("CIP-113");

            assertThat(apiDocs.read("$.components.schemas.ProgrammableTokenCip113.properties.transfer_logic_script", Object.class)).isNotNull();
            assertThat(apiDocs.read("$.components.schemas.ProgrammableTokenCip113.properties.third_party_transfer_logic_script", Object.class)).isNotNull();
            assertThat(apiDocs.read("$.components.schemas.ProgrammableTokenCip113.properties.global_state_policy_id", Object.class)).isNotNull();
        }
    }

    @Nested
    @DisplayName("Response schemas")
    class Schemas {

        @Test
        void shouldIncludeKeySchemas() {
            assertThat(apiDocs.read("$.components.schemas.TokenMetadata", Object.class)).isNotNull();
            assertThat(apiDocs.read("$.components.schemas.BatchRequest", Object.class)).isNotNull();
        }
    }

}
