package org.cardanofoundation.tokenmetadata.registry.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.model.Mapping;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@AllArgsConstructor
@Slf4j
public class TokenMetadataDao {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public static final String TOKEN_METADATA_INSERT_SQL = "INSERT INTO metadata " +
            "(subject, policy, name, ticker, url, description, decimals, updated, updated_by, properties) VALUES " +
            "(:subject, :policy, :name, :ticker, :url, :description, :decimals, :updated, :updated_by, :properties) " +
            "ON CONFLICT (subject) DO NOTHING";

    public static final String TOKEN_LOGO_INSERT_SQL = "INSERT INTO logo " +
            "(subject, logo) VALUES (:subject, :logo) " +
            "ON CONFLICT (subject) DO NOTHING";

    public void insertTokenMetadata(String subject, Optional<String> policy,
                                    Optional<String> name, Optional<String> ticker, Optional<String> url,
                                    Optional<String> description, Optional<Integer> decimals, Timestamp updatedAt, String updatedBy, Mapping mapping) {

        PGobject jsonBMapping = new PGobject();
        jsonBMapping.setType("jsonb");
        try {
            jsonBMapping.setValue(objectMapper.writeValueAsString(mapping));
        } catch (SQLException | JsonProcessingException e) {
            log.warn("Error", e);
        }

        Map<String, Serializable> myParams = new HashMap<>();
        myParams.put("subject", subject);
        myParams.put("policy", policy.orElse(null));
        myParams.put("name", name.orElse(null));
        myParams.put("ticker", ticker.orElse(null));
        myParams.put("url", url.orElse(null));
        myParams.put("description", description.orElse(null));
        myParams.put("decimals", decimals.orElse(null));
        myParams.put("updated", updatedAt);
        myParams.put("updated_by", updatedBy);
        myParams.put("properties", jsonBMapping);
        final SqlParameterSource params = new MapSqlParameterSource(myParams);
        namedParameterJdbcTemplate.update(TOKEN_METADATA_INSERT_SQL, params);

    }

    public void insertTokenLogo(String subject, Optional<String> logo) {
        Map<String, Serializable> myParams = new HashMap<>();
        myParams.put("subject", subject);
        myParams.put("logo", logo.orElse(null));
        final SqlParameterSource params = new MapSqlParameterSource(myParams);
        namedParameterJdbcTemplate.update(TOKEN_LOGO_INSERT_SQL, params);

    }

}
