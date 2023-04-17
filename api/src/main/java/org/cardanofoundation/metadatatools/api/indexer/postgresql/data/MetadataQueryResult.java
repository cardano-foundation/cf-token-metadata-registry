package org.cardanofoundation.metadatatools.api.indexer.postgresql.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import lombok.extern.log4j.Log4j2;
import org.cardanofoundation.metadatatools.api.model.rest.TokenMetadata;
import org.postgresql.util.PGobject;

import jakarta.validation.constraints.NotNull;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Log4j2
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetadataQueryResult {
  public static final String DEFAULT_QUERY_STRING =
      "SELECT subject, properties, updated, updated_by FROM metadata";
  public static final List<String> DEFAULT_PROPERTY_NAMES =
      Arrays.asList("name", "ticker", "url", "description", "logo", "decimals", "tools");
  static final ObjectMapper VALUE_MAPPER = new ObjectMapper();

  private String subject;
  private String policy;
  private String name;
  private String ticker;
  private String url;
  private String description;
  private String logo;
  private Integer decimals;
  private Date updated;
  private String updatedBy;
  private String properties;

  public final TokenMetadata toTokenMetadata() {
    return this.toTokenMetadata(new ArrayList<>());
  }

  public final TokenMetadata toTokenMetadata(@NotNull final List<String> fieldsToExclude) {
    if (this.properties != null) {
      try {
        final TokenMetadata tokenMetadata =
            VALUE_MAPPER.readValue(this.properties, TokenMetadata.class);
        for (final String fieldName : fieldsToExclude) {
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
        if (!fieldsToExclude.contains("updated")) {
          tokenMetadata.setUpdated(this.updated);
        }
        if (!fieldsToExclude.contains("updatedBy")) {
          tokenMetadata.setUpdatedBy(this.updatedBy);
        }
        return tokenMetadata;
      } catch (final JsonProcessingException e) {
        throw new IllegalStateException(
            String.format(
                "Properties object of record related to subject %s is invalid.", this.subject),
            e);
      }
    } else {
      throw new IllegalStateException(
          String.format(
              "Record related to the subject %s does not store its properties properly.",
              this.subject));
    }
  }

  /**
   * Map a MetadataQueryResult object from a SQL ResultSet based on assumed column names within that
   * ResultSet.
   *
   * @param resultSet The SQL ResultSet containing information to fill the MetadataQueryResult
   *     object returned by this function.
   * @return The MetadataQueryResult object filled with the data from the given ResultSet object.
   */
  public static MetadataQueryResult fromSubjectAndPropertiesResultSet(final ResultSet resultSet) {
    final MetadataQueryResult queryResult = new MetadataQueryResult();
    queryResult.setSubject(getColumnValue(resultSet, "subject", null, String.class));
    queryResult.setProperties(
        getColumnValue(resultSet, "properties", null, PGobject.class).getValue());
    queryResult.setUpdated(getColumnValue(resultSet, "updated", null, Date.class));
    queryResult.setUpdatedBy(getColumnValue(resultSet, "updated_by", null, String.class));
    return queryResult;
  }

  /**
   * Map a MetadataQueryResult object from a SQL ResultSet based on assumed column names within that
   * ResultSet.
   *
   * @param resultSet The SQL ResultSet containing information to fill the MetadataQueryResult
   *     object returned by this function.
   * @return The MetadataQueryResult object filled with the data from the given ResultSet object.
   */
  public static MetadataQueryResult fromSqlResultSet(final ResultSet resultSet) {
    final MetadataQueryResult queryResult = new MetadataQueryResult();
    queryResult.setSubject(getColumnValue(resultSet, "subject", null, String.class));
    queryResult.setPolicy(getColumnValue(resultSet, "policy", null, String.class));
    queryResult.setName(getColumnValue(resultSet, "name", null, String.class));
    queryResult.setTicker(getColumnValue(resultSet, "ticker", null, String.class));
    queryResult.setUrl(getColumnValue(resultSet, "url", null, String.class));
    queryResult.setDescription(getColumnValue(resultSet, "description", null, String.class));
    queryResult.setLogo(getColumnValue(resultSet, "logo", null, String.class));
    queryResult.setDecimals(getColumnValue(resultSet, "decimals", null, Integer.class));
    queryResult.setUpdated(getColumnValue(resultSet, "updated", null, Date.class));
    queryResult.setUpdatedBy(getColumnValue(resultSet, "updated_by", null, String.class));
    queryResult.setProperties(
        getColumnValue(resultSet, "properties", null, PGobject.class).getValue());
    return queryResult;
  }

  /**
   * Helper function that safely extracts data from a SQL ResultSet if available. Returns a
   * defaultValue otherwise.
   *
   * @param resultSet The SQL ResultSet probably containing the requested data.
   * @param columnName The name (label) of the column of interest.
   * @param defaultValue The default value that shall be returned if the column does not exist.
   * @param type The type of the column data.
   * @param <T> Expected type of the column data.
   * @return The column data as T or the default value if the column does not exist.
   */
  private static <T> T getColumnValue(
      final ResultSet resultSet,
      final String columnName,
      final T defaultValue,
      final Class<T> type) {
    try {
      return resultSet.getObject(columnName, type);
    } catch (final SQLException e) {
      return defaultValue;
    }
  }
}
