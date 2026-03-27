package org.cardanofoundation.tokenmetadata.registry.api.controller;

import org.cardanofoundation.tokenmetadata.registry.api.indexer.V1ApiMetadataIndexer;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.BatchRequest;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.BatchResponse;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.TokenMetadata;
import org.cardanofoundation.tokenmetadata.registry.api.service.RegistryMetricsService;
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
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MetadataApiController")
class MetadataApiControllerTest {

    @Mock
    private V1ApiMetadataIndexer v1ApiMetadataIndexer;

    @Mock
    private RegistryMetricsService metricsService;

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
            TokenMetadata metadata = TokenMetadata.builder().subject("abc123").build();
            when(v1ApiMetadataIndexer.findSubjectsSelectProperties(eq(List.of("abc123")), eq(List.of())))
                    .thenReturn(Map.of("abc123", metadata));

            BatchRequest request = new BatchRequest(List.of("abc123"), null);
            ResponseEntity<BatchResponse> response = controller.getSubjects(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getSubjects()).hasSize(1);
        }

        @Test
        void noSubjectsFound_returnsNoContent() {
            when(v1ApiMetadataIndexer.findSubjectsSelectProperties(any(), any()))
                    .thenReturn(Map.of());

            BatchRequest request = new BatchRequest(List.of("unknown"), List.of());
            ResponseEntity<BatchResponse> response = controller.getSubjects(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }

        @Test
        void illegalArgument_returnsBadRequest() {
            when(v1ApiMetadataIndexer.findSubjectsSelectProperties(any(), any()))
                    .thenThrow(new IllegalArgumentException("bad"));

            BatchRequest request = new BatchRequest(List.of("bad"), List.of());
            ResponseEntity<BatchResponse> response = controller.getSubjects(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("getAllPropertiesForSubject")
    class GetAllPropertiesForSubject {

        @Test
        void found_returnsOk() {
            TokenMetadata metadata = TokenMetadata.builder().subject("abc123").build();
            when(v1ApiMetadataIndexer.findSubject("abc123")).thenReturn(Optional.of(metadata));

            ResponseEntity<TokenMetadata> response = controller.getAllPropertiesForSubject("abc123");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(metadata);
        }

        @Test
        void notFound_returnsNoContent() {
            when(v1ApiMetadataIndexer.findSubject("unknown")).thenReturn(Optional.empty());

            ResponseEntity<TokenMetadata> response = controller.getAllPropertiesForSubject("unknown");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(metricsService).recordNotFound();
        }

        @Test
        void illegalArgument_returnsBadRequest() {
            when(v1ApiMetadataIndexer.findSubject(any())).thenThrow(new IllegalArgumentException("bad"));

            ResponseEntity<TokenMetadata> response = controller.getAllPropertiesForSubject("bad");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("getPropertyForSubject")
    class GetPropertyForSubject {

        @Test
        void found_returnsOk() {
            TokenMetadata metadata = TokenMetadata.builder().subject("abc123").build();
            when(v1ApiMetadataIndexer.findSubjectSelectProperties("abc123", List.of("ticker")))
                    .thenReturn(Optional.of(metadata));

            ResponseEntity<TokenMetadata> response = controller.getPropertyForSubject("abc123", "ticker");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        void notFound_returnsNoContent() {
            when(v1ApiMetadataIndexer.findSubjectSelectProperties("unknown", List.of("ticker")))
                    .thenReturn(Optional.empty());

            ResponseEntity<TokenMetadata> response = controller.getPropertyForSubject("unknown", "ticker");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(metricsService).recordNotFound();
        }

        @Test
        void illegalArgument_returnsBadRequest() {
            when(v1ApiMetadataIndexer.findSubjectSelectProperties(any(), any()))
                    .thenThrow(new IllegalArgumentException("bad"));

            ResponseEntity<TokenMetadata> response = controller.getPropertyForSubject("bad", "ticker");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

}
