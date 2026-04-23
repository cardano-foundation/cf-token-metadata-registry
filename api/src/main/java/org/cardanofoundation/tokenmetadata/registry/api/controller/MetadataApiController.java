package org.cardanofoundation.tokenmetadata.registry.api.controller;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.Cip26StorageReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.BatchRequest;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.BatchResponse;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.TokenMetadata;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@CrossOrigin
@RequestMapping("${openapi.metadataServer.base-path:}")
@Slf4j
@RequiredArgsConstructor
public class MetadataApiController implements MetadataApi {

    private final Cip26StorageReader cip26StorageReader;

    @Override
    public ResponseEntity<BatchResponse> getSubjects(final BatchRequest body) {
        if (body.getSubjects().isEmpty()) {
            return ResponseEntity.ok(new BatchResponse());
        }
        List<String> propertyFilter = body.getProperties();
        List<TokenMetadata> subjects = cip26StorageReader.findBySubjects(body.getSubjects()).stream()
                .map(entity -> V1TokenMetadataMapper.toDto(entity, propertyFilter))
                .toList();
        if (subjects.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        BatchResponse response = new BatchResponse();
        response.setSubjects(subjects);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<TokenMetadata> getAllPropertiesForSubject(final String subject) {
        return cip26StorageReader.findBySubject(subject)
                .map(entity -> ResponseEntity.ok(V1TokenMetadataMapper.toDto(entity, null)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @Override
    public ResponseEntity<TokenMetadata> getPropertyForSubject(final String subject, final String property) {
        return cip26StorageReader.findBySubject(subject)
                .map(entity -> ResponseEntity.ok(V1TokenMetadataMapper.toDto(entity, List.of(property))))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
