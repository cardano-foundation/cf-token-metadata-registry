package org.cardanofoundation.tokenmetadata.registry.api.controller;

import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.api.indexer.V1ApiMetadataIndexer;
import org.cardanofoundation.tokenmetadata.registry.api.indexer.V2ApiMetadataIndexer;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.TokenMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
@Controller
@CrossOrigin(exposedHeaders = {"X-Total-Count"})
@RequestMapping("${openapi.metadataServer.base-path:}")
public class V2ApiController implements V2Api {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 200;
    private static final Pattern HEXADECIMAL_PATTERN = Pattern.compile("\\p{XDigit}+");

    @Autowired
    private V1ApiMetadataIndexer v1ApiMetadataIndexer;

    @Autowired
    private V2ApiMetadataIndexer v2ApiMetadataIndexer;

    private static String sanitizeNetworkRequestParameter(final String network) {
        return network.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public ResponseEntity<TokenMetadata> getSubjectV2(final String subject, final String fields) {
        try {
            final Optional<TokenMetadata> queryResult;
            if (fields != null) {
                queryResult = v1ApiMetadataIndexer.findSubjectSelectProperties(subject, List.of(fields.trim().split(",")));
            } else {
                queryResult = v1ApiMetadataIndexer.findSubject(subject);
            }
            return queryResult
                    .map(tokenMetadata -> new ResponseEntity<>(tokenMetadata, HttpStatus.OK))
                    .orElseGet(() -> new ResponseEntity<>(HttpStatus.NO_CONTENT));
        } catch (final IllegalArgumentException e) {
            log.error("Not able to process request for single subject.", e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (final IllegalStateException e) {
            log.error("Could not process query result.", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
