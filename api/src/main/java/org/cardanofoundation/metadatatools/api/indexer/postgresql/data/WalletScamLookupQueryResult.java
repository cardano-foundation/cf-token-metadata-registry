package org.cardanofoundation.metadatatools.api.indexer.postgresql.data;

import lombok.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletScamLookupQueryResult {
  private String walletHash;
  private Integer scamIncidentId;
  private String domain;
  private Date reported;

  public static WalletScamLookupQueryResult fromSqlResultSet(final ResultSet resultSet) {
    final WalletScamLookupQueryResult queryResult = new WalletScamLookupQueryResult();
    queryResult.setWalletHash(getColumnValue(resultSet, "wallet_hash", null, String.class));
    queryResult.setScamIncidentId(
        getColumnValue(resultSet, "scam_incident_id", null, Integer.class));
    queryResult.setDomain(getColumnValue(resultSet, "domain", null, String.class));
    queryResult.setReported(getColumnValue(resultSet, "reported", null, Date.class));
    return queryResult;
  }

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
