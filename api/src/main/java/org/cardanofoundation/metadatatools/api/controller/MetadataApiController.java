package org.cardanofoundation.metadatatools.api.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.extern.log4j.Log4j2;
import org.cardanofoundation.metadatatools.api.config.OffchainMetadataRegistryConfig;
import org.cardanofoundation.metadatatools.api.indexer.V1ApiMetadataIndexer;
import org.cardanofoundation.metadatatools.api.model.rest.BatchRequest;
import org.cardanofoundation.metadatatools.api.model.rest.BatchResponse;
import org.cardanofoundation.metadatatools.api.model.rest.TokenMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@CrossOrigin
@RequestMapping("${openapi.metadataServer.base-path:}")
@Log4j2
public class MetadataApiController implements MetadataApi {
  @Autowired private V1ApiMetadataIndexer v1ApiMetadataIndexer;

  @Autowired private OffchainMetadataRegistryConfig offchainMetadataRegistryConfig;

  @Override
  public ResponseEntity<BatchResponse> getSubjects(
      @NotNull final BatchRequest body, final String network) {
    try {
      final Map<String, TokenMetadata> subjects =
          v1ApiMetadataIndexer.findSubjectsSelectProperties(
              offchainMetadataRegistryConfig.sourceFromNetwork(
                  network.trim().toLowerCase(Locale.ROOT)),
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
  public ResponseEntity<TokenMetadata> getAllPropertiesForSubject(
      @NotNull final String subject, final String network) {
    try {
      return v1ApiMetadataIndexer
          .findSubject(
              offchainMetadataRegistryConfig.sourceFromNetwork(
                  network.trim().toLowerCase(Locale.ROOT)),
              subject)
          .map(tokenMetadata -> new ResponseEntity<>(tokenMetadata, HttpStatus.OK))
          .orElseGet(() -> new ResponseEntity<>(HttpStatus.NO_CONTENT));
    } catch (final IllegalArgumentException e) {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
  }

  @Override
  public ResponseEntity<TokenMetadata> getPropertyForSubject(
      @NotNull final String subject, @NotNull final String property, final String network) {
    try {
      return v1ApiMetadataIndexer
          .findSubjectSelectProperties(
              offchainMetadataRegistryConfig.sourceFromNetwork(
                  network.trim().toLowerCase(Locale.ROOT)),
              subject,
              List.of(property))
          .map(tokenMetadata -> new ResponseEntity<>(tokenMetadata, HttpStatus.OK))
          .orElseGet(() -> new ResponseEntity<>(HttpStatus.NO_CONTENT));
    } catch (final IllegalArgumentException e) {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
  }
}
