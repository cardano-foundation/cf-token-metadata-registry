package org.cardanofoundation.tokenmetadata.registry.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.api.indexer.V1ApiMetadataIndexer;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.BatchRequest;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.BatchResponse;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.TokenMetadata;
import org.cardanofoundation.tokenmetadata.registry.api.service.MetricsService;
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
@RequiredArgsConstructor
public class MetadataApiController implements MetadataApi {

    private final V1ApiMetadataIndexer v1ApiMetadataIndexer;
    private final MetricsService metricsService;

    @Override
    public ResponseEntity<BatchResponse> getSubjects(final BatchRequest body) {
        try {
            if (body.getSubjects().isEmpty()) {
                final BatchResponse response = new BatchResponse();
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                metricsService.recordV1Query(body.getSubjects().size());
                final Map<String, TokenMetadata> subjects = v1ApiMetadataIndexer.findSubjectsSelectProperties(
                        body.getSubjects(),
                        body.getProperties() == null ? List.of() : body.getProperties());
                if (subjects.isEmpty()) {
                    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
                } else {
                    final BatchResponse response = new BatchResponse();
                    response.setSubjects(new ArrayList<>(subjects.values()));
                    return new ResponseEntity<>(response, HttpStatus.OK);
                }
            }

        } catch (final IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    public ResponseEntity<TokenMetadata> getAllPropertiesForSubject(final String subject) {
        try {
            metricsService.recordV1Query(1);
            return v1ApiMetadataIndexer
                    .findSubject(subject)
                    .map(tokenMetadata -> new ResponseEntity<>(tokenMetadata, HttpStatus.OK))
                    .orElseGet(() -> {
                        metricsService.recordNotFound();
                        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
                    });
        } catch (final IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    public ResponseEntity<TokenMetadata> getPropertyForSubject(final String subject, final String property) {
        try {
            metricsService.recordV1Query(1);
            return v1ApiMetadataIndexer
                    .findSubjectSelectProperties(subject, List.of(property))
                    .map(tokenMetadata -> new ResponseEntity<>(tokenMetadata, HttpStatus.OK))
                    .orElseGet(() -> {
                        metricsService.recordNotFound();
                        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
                    });
        } catch (final IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
}
