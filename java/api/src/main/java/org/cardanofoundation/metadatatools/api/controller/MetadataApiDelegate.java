package org.cardanofoundation.metadatatools.api.controller;

import org.cardanofoundation.metadatatools.api.model.BatchRequest;
import org.cardanofoundation.metadatatools.api.model.BatchResponse;
import org.cardanofoundation.metadatatools.api.model.Property;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.Optional;

/**
 * A delegate to be called by the {@link MetadataApiController}}.
 * Implement this interface with a {@link org.springframework.stereotype.Service} annotated class.
 */
public interface MetadataApiDelegate {

    default Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    default ResponseEntity<Property> getAllPropertiesForSubject(final String subject) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    default ResponseEntity<Property> getPropertyForSubject(final String subject, final String properties) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    default ResponseEntity<BatchResponse> getSubjects(final BatchRequest body) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }
}
