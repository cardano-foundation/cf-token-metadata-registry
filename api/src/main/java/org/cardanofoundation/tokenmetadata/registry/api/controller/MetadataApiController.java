package org.cardanofoundation.tokenmetadata.registry.api.controller;

import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.api.indexer.V1ApiMetadataIndexer;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.BatchRequest;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.BatchResponse;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.TokenMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@CrossOrigin
@RequestMapping("${openapi.metadataServer.base-path:}")
@Slf4j
public class MetadataApiController implements MetadataApi {
    @Autowired
    private V1ApiMetadataIndexer v1ApiMetadataIndexer;

    @Override
    public ResponseEntity<BatchResponse> getSubjects(final BatchRequest body) {
        try {
            final Map<String, TokenMetadata> subjects = v1ApiMetadataIndexer.findSubjectsSelectProperties(
                    body.getSubjects(),
                    body.getProperties());
            if (subjects.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            } else {
                final BatchResponse response = new BatchResponse();
                response.setSubjects(new ArrayList<>(subjects.values()));
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
        } catch (final IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    public ResponseEntity<TokenMetadata> getAllPropertiesForSubject(final String subject) {
        try {
            return v1ApiMetadataIndexer
                    .findSubject(subject)
                    .map(tokenMetadata -> new ResponseEntity<>(tokenMetadata, HttpStatus.OK))
                    .orElseGet(() -> new ResponseEntity<>(HttpStatus.NO_CONTENT));
        } catch (final IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    public ResponseEntity<TokenMetadata> getPropertyForSubject(final String subject, final String property) {
        try {
            return v1ApiMetadataIndexer
                    .findSubjectSelectProperties(subject, List.of(property))
                    .map(tokenMetadata -> new ResponseEntity<>(tokenMetadata, HttpStatus.OK))
                    .orElseGet(() -> new ResponseEntity<>(HttpStatus.NO_CONTENT));
        } catch (final IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
}
