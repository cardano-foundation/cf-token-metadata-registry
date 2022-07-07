package org.cardanofoundation.metadatatools.api.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.cardanofoundation.metadatatools.api.model.data.MetadataQueryResult;
import org.cardanofoundation.metadatatools.api.model.rest.BatchRequest;
import org.cardanofoundation.metadatatools.api.model.rest.BatchResponse;
import org.cardanofoundation.metadatatools.api.model.rest.TokenMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.validation.constraints.NotNull;
import java.util.*;

import static java.util.Map.entry;

@Controller
@CrossOrigin
@RequestMapping("${openapi.metadataServer.base-path:}")
@Log4j2
public class MetadataApiController implements MetadataApi {
    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    private static Map<String, TokenMetadata> metadataFromQueryResults(@NotNull final List<MetadataQueryResult> queryResults,
                                                                       @NotNull final List<String> properties)
            throws IllegalArgumentException, JsonProcessingException {
        final ObjectMapper valueMapper = new ObjectMapper();
        final Map<String, TokenMetadata> metadata = new HashMap<>();
        final List<String> propertiesToExclude = (properties.isEmpty()) ? new ArrayList<>() : new ArrayList<>(MetadataQueryResult.DEFAULT_PROPERTY_NAMES);
        propertiesToExclude.removeAll(properties);
        for (final MetadataQueryResult metadataQueryResult : queryResults) {
            if (metadataQueryResult.getProperties() != null) {
                try {
                    final TokenMetadata tokenMetadata = valueMapper.readValue(metadataQueryResult.getProperties(), TokenMetadata.class);
                    if (!propertiesToExclude.isEmpty()) {
                        for (final String fieldName : propertiesToExclude) {
                            switch (fieldName) {
                                case "name" -> tokenMetadata.setName(null);
                                case "ticker" -> tokenMetadata.setTicker(null);
                                case "url" -> tokenMetadata.setUrl(null);
                                case "description" -> tokenMetadata.setDescription(null);
                                case "decimals" -> tokenMetadata.setDecimals(null);
                                case "logo" -> tokenMetadata.setLogo(null);
                                default -> tokenMetadata.removeProperty(fieldName);
                            }
                        }
                    }
                    metadata.put(metadataQueryResult.getSubject(), tokenMetadata);
                } catch (final JsonProcessingException e) {
                    log.warn(String.format("Could not parse properties of subject %s.", metadataQueryResult.getSubject()), e);
                }
            }
        }
        return metadata;
    }

    /**
     * Convert a list of MetadataQueryResult's to response entities.
     *
     * @param queryResults List of MetadataQueryResult objects containing information about metadata mappings.
     * @return A Map containing response Property objects mapped to their subject name.
     * @throws IllegalArgumentException If no or an invalid value is related to a property of a subject.
     * @throws JsonProcessingException  If the value content is no well formed JSON.
     */
    private static Map<String, TokenMetadata> metadataFromQueryResults(@NotNull final List<MetadataQueryResult> queryResults)
            throws IllegalArgumentException, JsonProcessingException {
        return metadataFromQueryResults(queryResults, new ArrayList<>());
    }

    @Override
    public ResponseEntity<BatchResponse> getSubjects(@NotNull final BatchRequest body) {
        final SqlParameterSource params = new MapSqlParameterSource(Map.ofEntries(entry("subjects", body.getSubjects())));
        final String queryStatement = String.format("%s WHERE subject in (:subjects)", MetadataQueryResult.DEFAULT_QUERY_STRING);

        try {
            final List<MetadataQueryResult> queryResults = jdbcTemplate.query(queryStatement, params, (rs, rowNum) -> MetadataQueryResult.fromSubjectAndPropertiesResultSet(rs));
            final Map<String, TokenMetadata> subjects = metadataFromQueryResults(queryResults, body.getProperties());
            if (subjects.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            } else {
                final BatchResponse response = new BatchResponse();
                response.setSubjects(new ArrayList<>(subjects.values()));
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
        } catch (final JsonProcessingException | IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<TokenMetadata> getAllPropertiesForSubject(@NotNull final String subject) {
        try {
            final SqlParameterSource params = new MapSqlParameterSource("subject", subject);
            final List<MetadataQueryResult> queryResults = jdbcTemplate.query(String.format("%s WHERE subject = :subject", MetadataQueryResult.DEFAULT_QUERY_STRING), params, (rs, rowNum) -> MetadataQueryResult.fromSubjectAndPropertiesResultSet(rs));
            final Map<String, TokenMetadata> returnProperties = metadataFromQueryResults(queryResults);
            if (returnProperties.containsKey(subject)) {
                return new ResponseEntity<>(returnProperties.get(subject), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
        } catch (final IllegalArgumentException | JsonProcessingException e) {
            log.error("Could not query result to server response");
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<TokenMetadata> getPropertyForSubject(@NotNull final String subject, @NotNull final String properties) {
        try {
            final SqlParameterSource params = new MapSqlParameterSource(Map.ofEntries(entry("subject", subject)));
            final List<MetadataQueryResult> queryResults = jdbcTemplate.query(String.format("%s WHERE subject = :subject", MetadataQueryResult.DEFAULT_QUERY_STRING), params, (rs, rowNum) -> MetadataQueryResult.fromSubjectAndPropertiesResultSet(rs));
            final Map<String, TokenMetadata> returnProperties = metadataFromQueryResults(queryResults, List.of(properties));
            if (returnProperties.containsKey(subject)) {
                return new ResponseEntity<>(returnProperties.get(subject), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
        } catch (final IllegalArgumentException | JsonProcessingException e) {
            log.error("Could not query result to server response");
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
