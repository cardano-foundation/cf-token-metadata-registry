package org.cardanofoundation.tokenmetadata.registry.api.controller;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.Item;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.Mapping;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.Cip26StorageReader;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model.TokenMetadata;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.BatchRequest;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.BatchResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MetadataApiController")
class MetadataApiControllerTest {

    @Mock
    private Cip26StorageReader cip26StorageReader;

    @InjectMocks
    private MetadataApiController controller;

    @Nested
    @DisplayName("getSubjects (batch)")
    class GetSubjects {

        @Test
        void emptySubjectList_returnsEmptyResponse() {
            BatchRequest request = new BatchRequest(List.of(), List.of());
            ResponseEntity<BatchResponse> response = controller.getSubjects(request);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
        }

        @Test
        void foundSubjects_returnsOk() {
            TokenMetadata entity = newEntity("abc123", "MyToken");
            when(cip26StorageReader.findBySubjects(List.of("abc123"))).thenReturn(List.of(entity));

            BatchRequest request = new BatchRequest(List.of("abc123"), null);
            ResponseEntity<BatchResponse> response = controller.getSubjects(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getSubjects()).hasSize(1);
            assertThat(response.getBody().getSubjects().get(0).getSubject()).isEqualTo("abc123");
        }

        @Test
        void noSubjectsFound_returnsNoContent() {
            when(cip26StorageReader.findBySubjects(List.of("unknown"))).thenReturn(List.of());

            BatchRequest request = new BatchRequest(List.of("unknown"), List.of());
            ResponseEntity<BatchResponse> response = controller.getSubjects(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }
    }

    @Nested
    @DisplayName("getAllPropertiesForSubject")
    class GetAllPropertiesForSubject {

        @Test
        void found_returnsOk() {
            TokenMetadata entity = newEntity("abc123", "MyToken");
            when(cip26StorageReader.findBySubject("abc123")).thenReturn(Optional.of(entity));

            ResponseEntity<org.cardanofoundation.tokenmetadata.registry.api.model.rest.TokenMetadata> response =
                    controller.getAllPropertiesForSubject("abc123");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getSubject()).isEqualTo("abc123");
            assertThat(response.getBody().getName().getValue()).isEqualTo("MyToken");
        }

        @Test
        void notFound_returnsNoContent() {
            when(cip26StorageReader.findBySubject("unknown")).thenReturn(Optional.empty());
            ResponseEntity<org.cardanofoundation.tokenmetadata.registry.api.model.rest.TokenMetadata> response =
                    controller.getAllPropertiesForSubject("unknown");
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }
    }

    @Nested
    @DisplayName("getPropertyForSubject")
    class GetPropertyForSubject {

        @Test
        void found_returnsOk() {
            TokenMetadata entity = newEntity("abc123", "MyToken");
            when(cip26StorageReader.findBySubject("abc123")).thenReturn(Optional.of(entity));

            ResponseEntity<org.cardanofoundation.tokenmetadata.registry.api.model.rest.TokenMetadata> response =
                    controller.getPropertyForSubject("abc123", "name");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getName()).isNotNull();
            assertThat(response.getBody().getDescription()).isNull();
        }

        @Test
        void notFound_returnsNoContent() {
            when(cip26StorageReader.findBySubject("unknown")).thenReturn(Optional.empty());
            ResponseEntity<org.cardanofoundation.tokenmetadata.registry.api.model.rest.TokenMetadata> response =
                    controller.getPropertyForSubject("unknown", "name");
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }
    }

    private static TokenMetadata newEntity(String subject, String name) {
        TokenMetadata t = new TokenMetadata();
        t.setSubject(subject);
        Item nameItem = new Item(0, name, List.of());
        Item descItem = new Item(0, "A token", List.of());
        t.setProperties(new Mapping(subject, null, nameItem, null, null, null, null, descItem));
        return t;
    }

}
