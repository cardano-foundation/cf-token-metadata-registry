package org.cardanofoundation.tokenmetadata.registry.it;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The validate endpoint on a running registry (ADR-023): a token is checked before it exists, and nothing is stored.
 */
public class ValidateIntegrationIT extends BaseIntegrationIT {

    private static final String POLICY = "ab".repeat(28);

    @BeforeAll
    static void setUp() {
        waitForApiReady();
    }

    private static DocumentContext post(String standard, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = restTemplate.postForEntity(API_BASE_URL + "/api/v2/validate/" + standard,
                new HttpEntity<>(body, headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return JsonPath.parse(response.getBody());
    }

    @Test
    void buildsAndChecksACip68DatumFromTheTokenFields() {
        DocumentContext json = post("cip68", """
                {"fields": {"name": "Validator Token", "ticker": "VAL", "decimals": 6, "logo": "ipfs://bafy"}, "labels": [333]}""");

        assertThat(json.read("$.stored", Boolean.class)).isTrue();
        assertThat(json.read("$.datum", String.class)).isNotBlank();
        List<String> codes = json.read("$.served[0].validation_errors[*].code");
        assertThat(codes).containsExactly("missing_value");
        assertThat(json.read("$.served[0].validation_errors[0].field_name", String.class)).isEqualTo("description");
        List<String> scopes = json.read("$.served[0].scopes");
        assertThat(scopes).containsExactly("ALL");
    }

    @Test
    void reportsACip26FileNamedAfterAnotherSubject() {
        DocumentContext json = post("cip26", """
                {"mapping": {"subject": "%s4142", "name": {"value": "AB", "sequenceNumber": 0, "signatures": []},
                             "description": {"value": "A token", "sequenceNumber": 0, "signatures": []}},
                 "file_name": "%s4344.json"}""".formatted(POLICY, POLICY));

        assertThat(json.read("$.stored", Boolean.class)).isFalse();
        assertThat(json.read("$.dropped.code", String.class)).isEqualTo("filename_subject_mismatch");
    }

    @Test
    void reportsACip113NodeItCannotRead() {
        DocumentContext json = post("cip113", "{\"datum\": \"d87980\"}");

        assertThat(json.read("$.stored", Boolean.class)).isFalse();
        assertThat(json.read("$.dropped.code", String.class)).isEqualTo("registry_node_unparseable");
    }

    @Test
    void storesNothing() {
        post("cip68", "{\"fields\": {\"name\": \"Never Stored\", \"description\": \"x\"}}");

        assertThatThrownBy(() -> restTemplate.getForEntity(API_BASE_URL + "/api/v2/subjects/" + "00".repeat(28) + "0014df10", String.class))
                .isInstanceOf(HttpClientErrorException.NotFound.class);
    }
}
