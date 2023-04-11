package org.cardanofoundation.metadatatools.api.indexer.postgresql;

import static java.util.Map.entry;

import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.extern.log4j.Log4j2;
import org.cardanofoundation.metadatatools.api.indexer.ExtendedMetadataIndexer;
import org.cardanofoundation.metadatatools.api.indexer.postgresql.data.WalletScamLookupQueryResult;
import org.cardanofoundation.metadatatools.api.model.rest.WalletFraudIncident;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Component;

@Component
@Log4j2
public class ExtendedPostgresMetadataIndexer implements ExtendedMetadataIndexer {
  @Autowired private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

  @Override
  public List<WalletFraudIncident> findScamIncidents(
      @NotNull final List<String> walletAddressHashes) {
    if (walletAddressHashes.isEmpty()) {
      return List.of();
    }

    final List<WalletScamLookupQueryResult> queryResults;
    if (walletAddressHashes.size() == 1) {
      final SqlParameterSource params =
          new MapSqlParameterSource(
              Map.ofEntries(entry("addressHash", walletAddressHashes.get(0))));
      queryResults =
          namedParameterJdbcTemplate.query(
              "SELECT * FROM wallet_scam_lookup WHERE wallet_hash = :addressHash LIMIT 10",
              params,
              (rs, rowNum) -> WalletScamLookupQueryResult.fromSqlResultSet(rs));
    } else {
      final SqlParameterSource params =
          new MapSqlParameterSource(Map.ofEntries(entry("addressHashes", walletAddressHashes)));
      queryResults =
          namedParameterJdbcTemplate.query(
              "SELECT * FROM wallet_scam_lookup WHERE wallet_hash in (:addressHashes) LIMIT 10",
              params,
              (rs, rowNum) -> WalletScamLookupQueryResult.fromSqlResultSet(rs));
    }

    if (queryResults.isEmpty()) {
      return List.of();
    } else {
      return queryResults.stream()
          .map(
              scamLookupQueryResult -> {
                final WalletFraudIncident fraudIncident = new WalletFraudIncident();
                fraudIncident.setAddressHash(scamLookupQueryResult.getWalletHash());
                fraudIncident.setIncidentId(scamLookupQueryResult.getScamIncidentId());
                fraudIncident.setScamSiteDomain(scamLookupQueryResult.getDomain());
                fraudIncident.setReportedDate(
                    scamLookupQueryResult
                        .getReported()
                        .toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate());
                return fraudIncident;
              })
          .collect(Collectors.toList());
    }
  }
}
