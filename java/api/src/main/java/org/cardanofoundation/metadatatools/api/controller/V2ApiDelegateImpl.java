package org.cardanofoundation.metadatatools.api.controller;

import lombok.extern.log4j.Log4j2;
import org.cardanofoundation.metadatatools.api.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import static java.util.Map.entry;

@Service
@Log4j2
public class V2ApiDelegateImpl implements V2ApiDelegate {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 200;
    private static final String DEFAULT_ORDER_CLAUSE = "subject ASC";
    private static final List<String> VALID_SORTING_CRITERIA_NAMES = Arrays.asList(
            "subject",
            "policy",
            "name",
            "ticker",
            "url",
            "description",
            "decimals",
            "updated",
            "updatedBy");
    private static final Map<String, String> CRITERIA_COLUMN_NAME_MAPPING = Map.ofEntries(
            entry("subject", "subject"),
            entry("policy", "policy"),
            entry("name", "name"),
            entry("ticker", "ticker"),
            entry("url", "url"),
            entry("description", "description"),
            entry("decimals", "decimals"),
            entry("updated", "updated"),
            entry("updatedBy", "updated_by")
    );
    private static final String DEFAULT_CRITERIA_COLUMN_NAME = "subject";
    private static final Map<FilterOperand, String> OPERAND_NAME_MAPPING = Map.ofEntries(
            entry(FilterOperand.EQ, "="),
            entry(FilterOperand.NEQ, "<>"),
            entry(FilterOperand.LT, "<"),
            entry(FilterOperand.LTE, "<="),
            entry(FilterOperand.GT, ">"),
            entry(FilterOperand.GTE, ">=")
    );

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public ResponseEntity<Void> getHealthV2() {
        try {
            jdbcTemplate.execute("SELECT 1");
        } catch (final DataAccessException e) {
            log.error("Database connection is unhealthy", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Property> getSubjectV2(String subject, String fields) {
        try {
            final SqlParameterSource params = new MapSqlParameterSource(Map.ofEntries(entry("subject", subject)));
            final List<MetadataQueryResult> queryResults = namedParameterJdbcTemplate.query(String.format("%s WHERE subject = :subject", MetadataQueryResult.DEFAULT_QUERY_STRING), params, (rs, rowNum) -> MetadataQueryResult.fromSubjectAndPropertiesResultSet(rs));
            if (queryResults.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            } else if (queryResults.size() > 1) {
                System.out.printf("entries %d%n", queryResults.size());
                log.error("Got multiple rows by querying exact match for primary key. This should not happen.");
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            } else {
                final Property returnProperty;
                if (fields != null) {
                    final List<String> fieldsToExclude = new ArrayList<>(MetadataQueryResult.DEFAULT_PROPERTY_NAMES);
                    fieldsToExclude.removeAll(List.of(fields.split(",")));
                    returnProperty = queryResults.get(0).toProperty(fieldsToExclude);
                } else {
                    returnProperty = queryResults.get(0).toProperty();
                }
                if (returnProperty != null) {
                    return new ResponseEntity<>(returnProperty, HttpStatus.OK);
                } else {
                    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
                }
            }
        } catch (final IllegalArgumentException e) {
            log.error("Could not query result.");
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (final IllegalStateException e) {
            log.error("Could not process query result.");
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private static String orderClauseFromQueryParam(final String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return DEFAULT_ORDER_CLAUSE;
        } else {
            final String direction;
            final String sortCriteria;
            if (sortBy.startsWith("-")) {
                direction = "DESC";
                sortCriteria = sortBy.toLowerCase().substring(1);
            } else if (sortBy.startsWith("+")) {
                direction = "ASC";
                sortCriteria = sortBy.toLowerCase().substring(1);
            } else {
                direction = "ASC";
                sortCriteria = sortBy.toLowerCase();
            }

            return (VALID_SORTING_CRITERIA_NAMES.contains(sortCriteria))
                    ? ((sortCriteria.equals("subject"))
                    ? String.format("%s %s", columnNameFromCriteriaName(sortCriteria), direction)
                    : String.format("%s %s, subject ASC", columnNameFromCriteriaName(sortCriteria), direction))
                    : DEFAULT_ORDER_CLAUSE;
        }
    }

    private static String columnNameFromCriteriaName(@NotNull final String sortCriteria) {
        return CRITERIA_COLUMN_NAME_MAPPING.getOrDefault(sortCriteria, DEFAULT_CRITERIA_COLUMN_NAME);
    }

    private static int pageSizeFromLimitQueryParam(final Integer limit) {
        if (limit == null) {
            return DEFAULT_PAGE_SIZE;
        } else {
            return Math.max(MIN_PAGE_SIZE, Math.min(limit, MAX_PAGE_SIZE));
        }
    }

    private void computePageOffsetClause(final String pivotId,
                                         final String orderCriteriaParameter,
                                         @NotNull final Map<String, Object> sqlParamsSource,
                                         @NotNull final List<String> filterClauses) {
        if (pivotId != null) {
            try {
                final SqlParameterSource params = new MapSqlParameterSource(Map.ofEntries(entry("pivotid", pivotId)));
                final List<MetadataQueryResult> pivotElements = namedParameterJdbcTemplate.query("SELECT * FROM metadata WHERE subject = :pivotid", params, (rs, rowNum) -> MetadataQueryResult.fromSqlResultSet(rs));
                if (!pivotElements.isEmpty()) {
                    final String orderCriteriaNormalized;
                    final String orderDirectionOperator;
                    if (orderCriteriaParameter.startsWith("-")) {
                        orderDirectionOperator = "<=";
                        orderCriteriaNormalized = orderCriteriaParameter.toLowerCase().substring(1);
                    } else if (orderCriteriaParameter.startsWith("+")) {
                        orderDirectionOperator = ">=";
                        orderCriteriaNormalized = orderCriteriaParameter.toLowerCase().substring(1);
                    } else {
                        orderDirectionOperator = ">=";
                        orderCriteriaNormalized = orderCriteriaParameter.toLowerCase();
                    }
                    final String sortingColumnName = columnNameFromCriteriaName(orderCriteriaNormalized);
                    filterClauses.add(String.format("%s %s :afterid", sortingColumnName, orderDirectionOperator));
                    filterClauses.add("subject > :pivotid");
                    sqlParamsSource.put("afterid", computePivotCriteriaValue(pivotElements.get(0), sortingColumnName));
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
            default -> throw new IllegalArgumentException(String.format("Invalid sorting column name %s", sortingColumnName));
        };
    }

    private static void computeFulltextSearchClause(@NotNull final String q,
                                                    @NotNull final Map<String, Object> sqlParamsSource,
                                                    @NotNull final List<String> filterClauses) {
        filterClauses.add("textsearch @@ to_tsquery('english', :textquery)");
        sqlParamsSource.put("textquery", q);
    }

    private String whereClauseFromQueryParams(final String name,
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
                                              final String afterId,
                                              final String orderCriteria,
                                              final Map<String, Object> sqlParamsSource) {
        final List<String> filterClauses = new ArrayList<>();
        computePageOffsetClause(afterId, orderCriteria, sqlParamsSource, filterClauses);
        if (q != null && !q.isBlank()) {
            computeFulltextSearchClause(q, sqlParamsSource, filterClauses);
        } else {
            checkAndAddFilterCriteria("name", name, nameOp, filterClauses, sqlParamsSource);
            checkAndAddFilterCriteria("ticker", ticker, tickerOp, filterClauses, sqlParamsSource);
            checkAndAddFilterCriteria("description", description, descriptionOp, filterClauses, sqlParamsSource);
            checkAndAddFilterCriteria("url", url, urlOp, filterClauses, sqlParamsSource);
            checkAndAddFilterCriteria("policy", policy, policyOp, filterClauses, sqlParamsSource);
            checkAndAddFilterCriteria("updated", updated, updatedOp, filterClauses, sqlParamsSource);
            checkAndAddFilterCriteria("updatedBy", updatedBy, updatedByOp, filterClauses, sqlParamsSource);
            checkAndAddFilterCriteria("decimals", decimals, decimalsOp, filterClauses, sqlParamsSource);
        }

        if (filterClauses.isEmpty()) {
            return "";
        } else {
            return "WHERE " + String.join(" AND ", filterClauses);
        }
    }

    private static void checkAndAddFilterCriteria(@NotNull final String criteriaName, final Object filterParam, final FilterOperand filterOperand, @NotNull final List<String> filterClauses, @NotNull final Map<String, Object> sqlParamsSource) {
        if (filterParam != null) {
            filterClauses.add(String.format("%s %s :%s", columnNameFromCriteriaName(criteriaName), operandFromOperandName((filterOperand == null) ? FilterOperand.EQ : filterOperand), criteriaName));
            sqlParamsSource.put(criteriaName, filterParam);
        }
    }

    private static String operandFromOperandName(final FilterOperand op) {
        return OPERAND_NAME_MAPPING.getOrDefault(op, "=");
    }

    @Override
    public ResponseEntity<SubjectsResponse> getSubjectsV2(String fields, String sortBy, String name, FilterOperand nameOp, String ticker, FilterOperand tickerOp, String description, FilterOperand descriptionOp, String url, FilterOperand urlOp, String policy, FilterOperand policyOp, LocalDate updated, FilterOperand updatedOp, String updatedBy, FilterOperand updatedbyOp, Integer decimals, FilterOperand decimalsOp, String q, String vkey, Integer limit, String afterId) {
        try {
            final int pageSize = pageSizeFromLimitQueryParam(limit);
            final String orderByClause = orderClauseFromQueryParam(sortBy);
            final Map<String, Object> sqlParamsSource = new HashMap<>();
            final String filterClause = whereClauseFromQueryParams(
                    name, nameOp, ticker, tickerOp, description, descriptionOp, url, urlOp, policy, policyOp, updated,
                    updatedOp, updatedBy, updatedbyOp, decimals, decimalsOp, q, afterId, sortBy, sqlParamsSource);
            final SqlParameterSource params = new MapSqlParameterSource(sqlParamsSource);
            final List<MetadataQueryResult> queryResults = namedParameterJdbcTemplate.query(String.format("SELECT * FROM metadata %s ORDER BY %s LIMIT %d", filterClause, orderByClause, pageSize), params, (rs, rowNum) -> MetadataQueryResult.fromSubjectAndPropertiesResultSet(rs));
            if (queryResults.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            } else {
                final List<String> fieldsToExclude = (fields == null)
                        ? new ArrayList<>()
                        : (new ArrayList<>(MetadataQueryResult.DEFAULT_PROPERTY_NAMES));
                if (fields != null && !fields.isBlank()) {
                    fieldsToExclude.removeAll(List.of(fields.split(",")));
                }

                final List<Property> properties = new ArrayList<>();
                for (final MetadataQueryResult metadataQueryResult : queryResults) {
                    properties.add(metadataQueryResult.toProperty(fieldsToExclude));
                }
                final SubjectsResponse subjectsResponse = new SubjectsResponse();
                subjectsResponse.setSubjects(properties);
                return new ResponseEntity<>(subjectsResponse, HttpStatus.OK);
            }
        } catch (final IllegalArgumentException e) {
            log.error("Could not query result.");
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (final IllegalStateException e) {
            log.error("Could not process query result.");
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<Property> postSubjectV2(String subject, Property property) {
        // 1. verfiy data
        // 2. submit as PR to Github or where-ever
        return V2ApiDelegate.super.postSubjectV2(subject, property);
    }

    private boolean propertyHasRequiredFields(final Property property) {
        return true;
    }
    @Override
    public ResponseEntity<Void> verifySubjectV2(String subject, Property property) {
        // apply validation rules

        // 0. subject must match subject in property
        if (!property.getSubject().equals(subject)) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        // 1. first bytes of subject should match the policyId (if any)
        if (property.getPolicy() != null && !property.getSubject().startsWith(property.getPolicy())) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        // 2. ticker name is not too long
        if (property.getTicker() != null) {
            //if (if (property.getTicker().getValue().isEmpty()) || property.getTicker().getValue().isEmpty())
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        // 3. name is not too long
        // 4. description is not too long
        // 5. logo size is not too big
        // 6. decimals is a sane value (>= 0 < X)
        // 7. url makes sense
        // 8. validate given signatures
        return V2ApiDelegate.super.verifySubjectV2(subject, property);
    }

    @Override
    public ResponseEntity<Property> deleteSubjectV2(String subject, String signature, String vkey) {
        // 1. verify signature (which is sig(subject | "VOID")) with given vkey
        return V2ApiDelegate.super.deleteSubjectV2(subject, signature, vkey);
    }

    @Override
    public ResponseEntity<WalletTrustCheckResponse> v2ForensicsWallet(String addresshash) {
        try {
            final Map<String, String> sqlParamsSource = Map.ofEntries(entry("addressHash", addresshash));
            final SqlParameterSource params = new MapSqlParameterSource(sqlParamsSource);
            final List<WalletScamLookupQueryResult> queryResults = namedParameterJdbcTemplate.query("SELECT * FROM wallet_scam_lookup WHERE wallet_hash = :addressHash LIMIT 10", params, (rs, rowNum) -> WalletScamLookupQueryResult.fromSqlResultSet(rs));
            if (queryResults.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            } else {
                final WalletTrustCheckResponse response = new WalletTrustCheckResponse();
                for (final WalletScamLookupQueryResult scamLookupQueryResult : queryResults) {
                    final WalletFraudIncident fraudIncident = new WalletFraudIncident();
                    fraudIncident.setAddressHash(scamLookupQueryResult.getWalletHash());
                    fraudIncident.setIncidentId(scamLookupQueryResult.getScamIncidentId());
                    fraudIncident.setScamSiteDomain(scamLookupQueryResult.getDomain());
                    fraudIncident.setReportedDate(scamLookupQueryResult.getReported().toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate());
                    response.addIncidentsItem(fraudIncident);
                }
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
        } catch (final IllegalArgumentException e) {
            log.error("Could not query result.");
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (final IllegalStateException e) {
            log.error("Could not process query result.");
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    @Override
    public ResponseEntity<WalletTrustCheckResponse> v2ForensicsWallets(WalletHashes walletHashes) {
        return V2ApiDelegate.super.v2ForensicsWallets(walletHashes);
    }
}
