package org.cardanofoundation.tokenmetadata.registry.api.indexer.postgresql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.cardanofoundation.tokenmetadata.registry.api.indexer.postgresql.data.MetadataQueryResult;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.TokenMetadata;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class V1ApiPostgresMetadataIndexerTest {

    /** Mainnet: a registry file named and written in upper case, stored under its lower-case subject (V8). */
    private static final String STORED_SUBJECT = "0b9bedfbfb62b21a2a41f581c2811ee74ee062ce32b0eca43964cf796d6174726978";
    private static final String FILE_SUBJECT = "0b9bedfbfb62b21a2a41f581c2811ee74ee062ce32b0eca43964cf796D6174726978";

    private final NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
    private final V1ApiPostgresMetadataIndexer indexer = new V1ApiPostgresMetadataIndexer(new ObjectMapper(), jdbcTemplate);

    @SuppressWarnings("unchecked")
    private void storedEntry() {
        MetadataQueryResult row = new MetadataQueryResult();
        row.setSubject(STORED_SUBJECT);
        row.setProperties("{\"subject\": \"" + FILE_SUBJECT + "\", \"name\": {\"value\": \"matrix\", \"sequenceNumber\": 0}}");
        when(jdbcTemplate.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of(row));
    }

    @Nested
    class ServedSubject {

        @Test
        void isTheStoredSubjectNotTheOneWrittenInTheRegistryFile() {
            storedEntry();

            assertThat(indexer.findSubject(STORED_SUBJECT)).hasValueSatisfying(metadata -> {
                assertThat(metadata.getSubject()).isEqualTo(STORED_SUBJECT);
                assertThat(metadata.getName().getValue()).isEqualTo("matrix");
            });
        }

        @Test
        void isTheStoredSubjectInABatchQueryToo() {
            storedEntry();

            Map<String, TokenMetadata> found = indexer.findSubjectsSelectProperties(List.of(STORED_SUBJECT), List.of());

            assertThat(found).containsOnlyKeys(STORED_SUBJECT);
            assertThat(found.get(STORED_SUBJECT).getSubject()).isEqualTo(STORED_SUBJECT);
        }
    }
}
