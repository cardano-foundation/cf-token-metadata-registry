package org.cardanofoundation.tokenmetadata.registry.api.indexer.postgresql;

import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.api.indexer.FetchMetadataResultSet;
import org.cardanofoundation.tokenmetadata.registry.api.indexer.V2ApiMetadataIndexer;
import org.cardanofoundation.tokenmetadata.registry.api.indexer.postgresql.data.MetadataQueryResult;
import org.cardanofoundation.tokenmetadata.registry.api.indexer.postgresql.data.WalletScamLookupQueryResult;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.FilterOperand;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.PivotDirection;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.TokenMetadata;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.WalletFraudIncident;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Map.entry;

@Component
@Slf4j
public class V2ApiPostgresMetadataIndexer implements V2ApiMetadataIndexer {
    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 200;
    private static final String DEFAULT_ORDER_CLAUSE = "subject ASC";
    private static final String DEFAULT_ORDER_CLAUSE_FLIPPED = "subject DESC";
    private static final List<String> VALID_SORTING_CRITERIA_NAMES =
            Arrays.asList("subject",
                    "policy",
                    "name",
                    "ticker",
                    "url",
                    "description",
                    "decimals",
                    "updated",
                    "updatedBy");
    private static final Map<String, String> CRITERIA_COLUMN_NAME_MAPPING =
            Map.ofEntries(
                    entry("subject", "subject"),
                    entry("policy", "policy"),
                    entry("name", "name"),
                    entry("ticker", "ticker"),
                    entry("url", "url"),
                    entry("description", "description"),
                    entry("decimals", "decimals"),
                    entry("updated", "updated"),
                    entry("updatedBy", "updated_by"));
    private static final String DEFAULT_CRITERIA_COLUMN_NAME = "subject";
    private static final Map<FilterOperand, String> OPERAND_NAME_MAPPING =
            Map.ofEntries(
                    entry(FilterOperand.EQ, "="),
                    entry(FilterOperand.NEQ, "<>"),
                    entry(FilterOperand.LT, "<"),
                    entry(FilterOperand.LTE, "<="),
                    entry(FilterOperand.GT, ">"),
                    entry(FilterOperand.GTE, ">="));

    private static String orderClauseFromQueryParam(final String sortBy, final boolean flipOrdering) {
        if (sortBy == null || sortBy.isBlank()) {
            return flipOrdering ? DEFAULT_ORDER_CLAUSE_FLIPPED : DEFAULT_ORDER_CLAUSE;
        } else {
            final String direction;
            final String sortCriteria;
            if (sortBy.startsWith("-")) {
                direction = flipOrdering ? "ASC" : "DESC";
                sortCriteria = sortBy.toLowerCase().substring(1);
            } else if (sortBy.startsWith("+")) {
                direction = flipOrdering ? "DESC" : "ASC";
                sortCriteria = sortBy.toLowerCase().substring(1);
            } else {
                direction = flipOrdering ? "DESC" : "ASC";
                sortCriteria = sortBy.toLowerCase();
            }

            return VALID_SORTING_CRITERIA_NAMES.contains(sortCriteria)
                    ? (sortCriteria.equals("subject")
                    ? String.format("%s %s", columnNameFromCriteriaName(sortCriteria), direction)
                    : String.format(
                    "%s %s, %s",
                    columnNameFromCriteriaName(sortCriteria),
                    direction,
                    flipOrdering ? DEFAULT_ORDER_CLAUSE_FLIPPED : DEFAULT_ORDER_CLAUSE))
                    : flipOrdering ? DEFAULT_ORDER_CLAUSE_FLIPPED : DEFAULT_ORDER_CLAUSE;
        }
    }

    private static String columnNameFromCriteriaName(@NotNull final String sortCriteria) {
        return CRITERIA_COLUMN_NAME_MAPPING.getOrDefault(sortCriteria, DEFAULT_CRITERIA_COLUMN_NAME);
    }

    private static int pageSizeFromLimitQueryParam(final Integer limit) {
        return (limit == null)
                ? DEFAULT_PAGE_SIZE
                : Math.max(MIN_PAGE_SIZE, Math.min(limit, MAX_PAGE_SIZE));
    }

    private void computePageOffsetClause(
            final String pivotId,
            final PivotDirection pivotDirection,
            final String orderCriteriaParameter,
            @NotNull final Map<String, Object> sqlParamsSource,
            @NotNull final List<String> filterClauses) {
        if (pivotId != null) {
            try {
                final SqlParameterSource params =
                        new MapSqlParameterSource(
                                Map.ofEntries(entry("pivotid", pivotId)));
                final List<MetadataQueryResult> pivotElements =
                        namedParameterJdbcTemplate.query(
                                "SELECT * FROM metadata WHERE subject = :pivotid",
                                params,
                                (rs, rowNum) -> MetadataQueryResult.fromSqlResultSet(rs));
                if (!pivotElements.isEmpty()) {
                    final String orderCriteriaNormalized;
                    final String orderDirectionOperator;
                    if (orderCriteriaParameter != null) {
                        if (orderCriteriaParameter.startsWith("-")) {
                            if (pivotDirection == PivotDirection.BEFORE) {
                                orderDirectionOperator = ">=";
                            } else {
                                orderDirectionOperator = "<=";
                            }
                            orderCriteriaNormalized = orderCriteriaParameter.toLowerCase().substring(1);
                        } else if (orderCriteriaParameter.startsWith("+")) {
                            if (pivotDirection == PivotDirection.AFTER) {
                                orderDirectionOperator = ">=";
                            } else {
                                orderDirectionOperator = "<=";
                            }
                            orderCriteriaNormalized = orderCriteriaParameter.toLowerCase().substring(1);
                        } else {
                            if (pivotDirection == PivotDirection.AFTER) {
                                orderDirectionOperator = ">=";
                            } else {
                                orderDirectionOperator = "<=";
                            }
                            orderCriteriaNormalized = orderCriteriaParameter.toLowerCase();
                        }

                        final String sortingColumnName = columnNameFromCriteriaName(orderCriteriaNormalized);
                        filterClauses.add(
                                String.format("%s %s :afterid", sortingColumnName, orderDirectionOperator));
                        sqlParamsSource.put(
                                "afterid", computePivotCriteriaValue(pivotElements.get(0), sortingColumnName));
                    }

                    if (pivotDirection == PivotDirection.AFTER) {
                        filterClauses.add("subject > :pivotid");
                    } else {
                        filterClauses.add("subject < :pivotid");
                    }
                    sqlParamsSource.put("pivotid", pivotId);
                } else {
                    log.warn("Cannot find pivot element for pagination.");
                }
            } catch (DataAccessException | IllegalArgumentException e) {
                log.warn("Cannot query pivot element for pagination.", e);
            }
        }
    }

    private Object computePivotCriteriaValue(final MetadataQueryResult metadataQueryResult, final String sortingColumnName) {
        return switch (sortingColumnName) {
            case "subject" -> metadataQueryResult.getSubject();
            case "policy" -> metadataQueryResult.getPolicy();
            case "name" -> metadataQueryResult.getName();
            case "ticker" -> metadataQueryResult.getTicker();
            case "url" -> metadataQueryResult.getUrl();
            case "description" -> metadataQueryResult.getDescription();
            case "decimals" -> metadataQueryResult.getDecimals();
            case "updated" -> metadataQueryResult.getUpdated();
            case "updated_by" -> metadataQueryResult.getUpdatedBy();
            default ->
                    throw new IllegalArgumentException(String.format("Invalid sorting column name %s", sortingColumnName));
        };
    }

    private static void computeFulltextSearchClause(
            @NotNull final String q,
            @NotNull final Map<String, Object> sqlParamsSource,
            @NotNull final List<String> filterClauses) {
        filterClauses.add("textsearch @@ to_tsquery('english', :textquery)");
        sqlParamsSource.put("textquery", q);
    }

    private String whereClauseFromQueryParams(
            final String name,
            final FilterOperand nameOp,
            final String ticker,
            final FilterOperand tickerOp,
            final String description,
            final FilterOperand descriptionOp,
            final String url,
            final FilterOperand urlOp,
            final String policy,
            final FilterOperand policyOp,
            final LocalDate updated,
            final FilterOperand updatedOp,
            final String updatedBy,
            final FilterOperand updatedByOp,
            final Integer decimals,
            final FilterOperand decimalsOp,
            final String q,
            final String pivotId,
            final PivotDirection pivotDirection,
            final String orderCriteria,
            final Map<String, Object> sqlParamsSource,
            final boolean noPageOffsetClause) {
        final List<String> filterClauses = new ArrayList<>();
        if (!noPageOffsetClause) {
            computePageOffsetClause(
                    pivotId, pivotDirection, orderCriteria, sqlParamsSource, filterClauses);
        }
        if (q != null && !q.isBlank()) {
            computeFulltextSearchClause(q, sqlParamsSource, filterClauses);
        } else {
            checkAndAddFilterCriteria("name", name, nameOp, filterClauses, sqlParamsSource);
            checkAndAddFilterCriteria("ticker", ticker, tickerOp, filterClauses, sqlParamsSource);
            checkAndAddFilterCriteria(
                    "description", description, descriptionOp, filterClauses, sqlParamsSource);
            checkAndAddFilterCriteria("url", url, urlOp, filterClauses, sqlParamsSource);
            checkAndAddFilterCriteria("policy", policy, policyOp, filterClauses, sqlParamsSource);
            checkAndAddFilterCriteria("updated", updated, updatedOp, filterClauses, sqlParamsSource);
            checkAndAddFilterCriteria(
                    "updatedBy", updatedBy, updatedByOp, filterClauses, sqlParamsSource);
            checkAndAddFilterCriteria("decimals", decimals, decimalsOp, filterClauses, sqlParamsSource);
        }

        if (filterClauses.isEmpty()) {
            return "";
        } else {
            return "WHERE " + String.join(" AND ", filterClauses);
        }
    }

    private static void checkAndAddFilterCriteria(
            @NotNull final String criteriaName,
            final Object filterParam,
            final FilterOperand filterOperand,
            @NotNull final List<String> filterClauses,
            @NotNull final Map<String, Object> sqlParamsSource) {
        if (filterParam != null) {
            filterClauses.add(
                    String.format(
                            "%s %s :%s",
                            columnNameFromCriteriaName(criteriaName),
                            operandFromOperandName((filterOperand == null) ? FilterOperand.EQ : filterOperand),
                            criteriaName));
            sqlParamsSource.put(criteriaName, filterParam);
        }
    }

    private static String operandFromOperandName(final FilterOperand op) {
        return OPERAND_NAME_MAPPING.getOrDefault(op, "=");
    }

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

    @Override
    public FetchMetadataResultSet fetchMetadata(
            final String fields,
            final String sortBy,
            final String name,
            final FilterOperand nameOp,
            final String ticker,
            final FilterOperand tickerOp,
            final String description,
            final FilterOperand descriptionOp,
            final String url,
            final FilterOperand urlOp,
            final String policy,
            final FilterOperand policyOp,
            final LocalDate updated,
            final FilterOperand updatedOp,
            final String updatedBy,
            final FilterOperand updatedbyOp,
            final Integer decimals,
            final FilterOperand decimalsOp,
            final String q,
            final String vkey,
            final Integer pageSize,
            final Long page,
            final String pivotId,
            final PivotDirection pivotDirection) {
        final Map<String, Object> sqlParamsSourceTotalCount = new HashMap<>();
        final String filterClauseTotalCount =
                whereClauseFromQueryParams(
                        name,
                        nameOp,
                        ticker,
                        tickerOp,
                        description,
                        descriptionOp,
                        url,
                        urlOp,
                        policy,
                        policyOp,
                        updated,
                        updatedOp,
                        updatedBy,
                        updatedbyOp,
                        decimals,
                        decimalsOp,
                        q,
                        pivotId,
                        pivotDirection,
                        sortBy,
                        sqlParamsSourceTotalCount,
                        true);
        final SqlParameterSource paramsTotalCountParameters =
                new MapSqlParameterSource(sqlParamsSourceTotalCount);
        final List<Long> totalResultSetCountQueryResult =
                namedParameterJdbcTemplate.query(
                        String.format("SELECT count(*) as cnt FROM metadata %s", filterClauseTotalCount),
                        paramsTotalCountParameters,
                        (rs, rowNum) -> rs.getLong("cnt"));
        final long totalResultSetCount =
                totalResultSetCountQueryResult.isEmpty() ? 0 : totalResultSetCountQueryResult.get(0);
        final long pageSanitized =
                (page != null) ? Math.min(totalResultSetCount / pageSize, Math.max(page, 0)) : 0;
        final String orderByClause =
                orderClauseFromQueryParam(
                        sortBy, pivotId != null && pivotDirection == PivotDirection.BEFORE);
        final Map<String, Object> sqlParamsSource = new HashMap<>();
        final String filterClause =
                whereClauseFromQueryParams(
                        name,
                        nameOp,
                        ticker,
                        tickerOp,
                        description,
                        descriptionOp,
                        url,
                        urlOp,
                        policy,
                        policyOp,
                        updated,
                        updatedOp,
                        updatedBy,
                        updatedbyOp,
                        decimals,
                        decimalsOp,
                        q,
                        pivotId,
                        pivotDirection,
                        sortBy,
                        sqlParamsSource,
                        false);
        final SqlParameterSource params = new MapSqlParameterSource(sqlParamsSource);
        final List<MetadataQueryResult> queryResults =
                namedParameterJdbcTemplate.query(
                        String.format(
                                "SELECT * FROM metadata %s ORDER BY %s LIMIT %d OFFSET %d",
                                filterClause, orderByClause, pageSize, pageSanitized * pageSize),
                        params,
                        (rs, rowNum) -> MetadataQueryResult.fromSqlResultSet(rs));
        if (queryResults.isEmpty()) {
            return FetchMetadataResultSet.builder().results(List.of()).totalCount(0).build();
        } else {
            final List<String> fieldsToExclude =
                    (fields == null)
                            ? new ArrayList<>()
                            : new ArrayList<>(MetadataQueryResult.DEFAULT_PROPERTY_NAMES);
            if (fields != null && !fields.isBlank()) {
                fieldsToExclude.removeAll(List.of(fields.split(",")));
            }

            final List<TokenMetadata> properties = new ArrayList<>();
            for (final MetadataQueryResult metadataQueryResult : queryResults) {
                properties.add(metadataQueryResult.toTokenMetadata(fieldsToExclude));
            }

            // reverse the order for backwards pagination
            if (pivotId != null && pivotDirection == PivotDirection.BEFORE) {
                Collections.reverse(properties);
            }
            return FetchMetadataResultSet.builder()
                    .results(properties)
                    .totalCount(totalResultSetCount)
                    .build();
        }
    }
}
