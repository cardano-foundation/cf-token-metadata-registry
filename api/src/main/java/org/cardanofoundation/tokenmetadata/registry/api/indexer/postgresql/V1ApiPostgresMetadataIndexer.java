package org.cardanofoundation.tokenmetadata.registry.api.indexer.postgresql;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.api.indexer.V1ApiMetadataIndexer;
import org.cardanofoundation.tokenmetadata.registry.api.indexer.postgresql.data.MetadataQueryResult;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.TokenMetadata;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Component;

import java.util.*;

import static java.util.Map.entry;

@Component
@RequiredArgsConstructor
@Slf4j
public class V1ApiPostgresMetadataIndexer implements V1ApiMetadataIndexer {

    private final ObjectMapper objectMapper;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private Map<String, TokenMetadata> metadataFromQueryResults(@NotNull final List<MetadataQueryResult> queryResults,
                                                                @NotNull final List<String> properties) throws IllegalArgumentException {
        final Map<String, TokenMetadata> metadata = new HashMap<>();
        final List<String> propertiesToExclude =
                properties.isEmpty()
                        ? new ArrayList<>()
                        : new ArrayList<>(MetadataQueryResult.DEFAULT_PROPERTY_NAMES);
        propertiesToExclude.removeAll(properties);
        for (final MetadataQueryResult metadataQueryResult : queryResults) {
            if (metadataQueryResult.getProperties() != null) {
                try {
                    final TokenMetadata tokenMetadata =
                            objectMapper.readValue(metadataQueryResult.getProperties(), TokenMetadata.class);
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
                    log.warn(
                            String.format(
                                    "Could not parse properties of subject %s.", metadataQueryResult.getSubject()),
                            e);
                }
            }
        }
        return metadata;
    }

    /**
     * Convert a list of MetadataQueryResult's to response entities.
     *
     * @param queryResults List of MetadataQueryResult objects containing information about metadata
     *                     mappings.
     * @return A Map containing response Property objects mapped to their subject name.
     * @throws IllegalArgumentException If no or an invalid value is related to a property of a
     *                                  subject.
     */
    private Map<String, TokenMetadata> metadataFromQueryResults(@NotNull final List<MetadataQueryResult> queryResults) throws IllegalArgumentException {
        return metadataFromQueryResults(queryResults, List.of());
    }

    @Override
    public Map<String, TokenMetadata> findSubjectsSelectProperties(List<String> subjects, List<String> properties) {
        final SqlParameterSource params = new MapSqlParameterSource(Map.ofEntries(entry("subjects", subjects)));
        final String queryStatement = String.format("%s WHERE subject in (:subjects)", MetadataQueryResult.DEFAULT_QUERY_STRING);
        final List<MetadataQueryResult> queryResults = jdbcTemplate.query(queryStatement,
                params,
                (rs, rowNum) -> MetadataQueryResult.fromSubjectAndPropertiesResultSet(rs));
        return metadataFromQueryResults(queryResults, properties);
    }

    @Override
    public Optional<TokenMetadata> findSubject(String subject) {
        final SqlParameterSource params = new MapSqlParameterSource(Map.ofEntries(entry("subject", subject)));
        final List<MetadataQueryResult> queryResults = jdbcTemplate.query(String.format("%s WHERE subject = :subject", MetadataQueryResult.DEFAULT_QUERY_STRING),
                params,
                (rs, rowNum) -> MetadataQueryResult.fromSubjectAndPropertiesResultSet(rs));
        return metadataFromQueryResults(queryResults).values().stream().findFirst();
    }

    @Override
    public Optional<TokenMetadata> findSubjectSelectProperties(String subject, List<String> properties) {
        final SqlParameterSource params = new MapSqlParameterSource(Map.ofEntries(entry("subject", subject)));
        final String queryStatement = String.format("%s WHERE subject = :subject", MetadataQueryResult.DEFAULT_QUERY_STRING);
        final List<MetadataQueryResult> queryResults = jdbcTemplate.query(queryStatement,
                params,
                (rs, rowNum) -> MetadataQueryResult.fromSubjectAndPropertiesResultSet(rs));
        return metadataFromQueryResults(queryResults, properties).values().stream().findFirst();
    }
}
