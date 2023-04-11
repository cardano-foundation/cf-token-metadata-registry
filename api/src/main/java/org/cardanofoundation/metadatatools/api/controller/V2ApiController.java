package org.cardanofoundation.metadatatools.api.controller;

import lombok.extern.log4j.Log4j2;
import org.cardanofoundation.metadatatools.api.config.OffchainMetadataRegistryConfig;
import org.cardanofoundation.metadatatools.api.indexer.ExtendedMetadataIndexer;
import org.cardanofoundation.metadatatools.api.indexer.SimpleMetadataIndexer;
import org.cardanofoundation.metadatatools.api.indexer.postgresql.data.MetadataQueryResult;
import org.cardanofoundation.metadatatools.api.model.rest.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Map.entry;

@Log4j2
@Controller
@CrossOrigin(exposedHeaders = {"X-Total-Count"})
@RequestMapping("${openapi.metadataServer.base-path:}")
public class V2ApiController implements V2Api {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 200;
    private static final String DEFAULT_ORDER_CLAUSE = "subject ASC";
    private static final String DEFAULT_ORDER_CLAUSE_FLIPPED = "subject DESC";
    private static final List<String> VALID_SORTING_CRITERIA_NAMES = Arrays.asList("subject", "policy", "name", "ticker", "url", "description", "decimals", "updated", "updatedBy");
    private static final Map<String, String> CRITERIA_COLUMN_NAME_MAPPING = Map.ofEntries(entry("subject", "subject"), entry("policy", "policy"), entry("name", "name"), entry("ticker", "ticker"), entry("url", "url"), entry("description", "description"), entry("decimals", "decimals"), entry("updated", "updated"), entry("updatedBy", "updated_by"));
    private static final String DEFAULT_CRITERIA_COLUMN_NAME = "subject";
    private static final Map<FilterOperand, String> OPERAND_NAME_MAPPING = Map.ofEntries(entry(FilterOperand.EQ, "="), entry(FilterOperand.NEQ, "<>"), entry(FilterOperand.LT, "<"), entry(FilterOperand.LTE, "<="), entry(FilterOperand.GT, ">"), entry(FilterOperand.GTE, ">="));
    private static final Pattern HEXADECIMAL_PATTERN = Pattern.compile("\\p{XDigit}+");

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OffchainMetadataRegistryConfig offchainMetadataRegistryConfig;

    @Autowired
    private SimpleMetadataIndexer simpleMetadataIndexer;

    @Autowired
    private ExtendedMetadataIndexer extendedMetadataIndexer;

    private static String sanitizeNetworkRequestParameter(final String network) {
        return network.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public ResponseEntity<TokenMetadata> getSubjectV2(final String subject, final String fields, final String network) {
        try {
            final Optional<TokenMetadata> queryResult;
            if (fields != null) {
                queryResult = simpleMetadataIndexer.findSubjectSelectProperties(offchainMetadataRegistryConfig.sourceFromNetwork(sanitizeNetworkRequestParameter(network)), subject, List.of(fields.trim().split(",")));
            } else {
                queryResult = simpleMetadataIndexer.findSubject(offchainMetadataRegistryConfig.sourceFromNetwork(sanitizeNetworkRequestParameter(network)), subject);
            }
            return queryResult.map(tokenMetadata -> new ResponseEntity<>(tokenMetadata, HttpStatus.OK)).orElseGet(() -> new ResponseEntity<>(HttpStatus.NO_CONTENT));
        } catch (final IllegalArgumentException e) {
            log.error("Not able to process request for single subject.", e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (final IllegalStateException e) {
            log.error("Could not process query result.", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

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

            return VALID_SORTING_CRITERIA_NAMES.contains(sortCriteria) ? (sortCriteria.equals("subject") ? String.format("%s %s", columnNameFromCriteriaName(sortCriteria), direction) : String.format("%s %s, %s", columnNameFromCriteriaName(sortCriteria), direction, flipOrdering ? DEFAULT_ORDER_CLAUSE_FLIPPED : DEFAULT_ORDER_CLAUSE)) : flipOrdering ? DEFAULT_ORDER_CLAUSE_FLIPPED : DEFAULT_ORDER_CLAUSE;
        }
    }

    private static String columnNameFromCriteriaName(@NotNull final String sortCriteria) {
        return CRITERIA_COLUMN_NAME_MAPPING.getOrDefault(sortCriteria, DEFAULT_CRITERIA_COLUMN_NAME);
    }

    private static int pageSizeFromLimitQueryParam(final Integer limit) {
        return (limit == null) ? DEFAULT_PAGE_SIZE : Math.max(MIN_PAGE_SIZE, Math.min(limit, MAX_PAGE_SIZE));
    }

    private void computePageOffsetClause(final String pivotId, final PivotDirection pivotDirection, final String metadataSource, final String orderCriteriaParameter, @NotNull final Map<String, Object> sqlParamsSource, @NotNull final List<String> filterClauses) {
        if (pivotId != null) {
            try {
                final SqlParameterSource params = new MapSqlParameterSource(Map.ofEntries(entry("pivotid", pivotId), entry("source", metadataSource)));
                final List<MetadataQueryResult> pivotElements = namedParameterJdbcTemplate.query("SELECT * FROM metadata WHERE subject = :pivotid AND source = :source", params, (rs, rowNum) -> MetadataQueryResult.fromSqlResultSet(rs));
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
                        filterClauses.add(String.format("%s %s :afterid", sortingColumnName, orderDirectionOperator));
                        sqlParamsSource.put("afterid", computePivotCriteriaValue(pivotElements.get(0), sortingColumnName));
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
            default -> throw new IllegalArgumentException(String.format("Invalid sorting column name %s", sortingColumnName));
        };
    }

    private static void computeFulltextSearchClause(@NotNull final String q, @NotNull final Map<String, Object> sqlParamsSource, @NotNull final List<String> filterClauses) {
        filterClauses.add("textsearch @@ to_tsquery('english', :textquery)");
        sqlParamsSource.put("textquery", q);
    }

    private String whereClauseFromQueryParams(final String name, final FilterOperand nameOp, final String ticker, final FilterOperand tickerOp, final String description, final FilterOperand descriptionOp, final String url, final FilterOperand urlOp, final String policy, final FilterOperand policyOp, final LocalDate updated, final FilterOperand updatedOp, final String updatedBy, final FilterOperand updatedByOp, final Integer decimals, final FilterOperand decimalsOp, final String q, final String pivotId, final PivotDirection pivotDirection, final String metadataSource, final String orderCriteria, final Map<String, Object> sqlParamsSource, final boolean noPageOffsetClause) {
        final List<String> filterClauses = new ArrayList<>();
        if (!noPageOffsetClause) {
            computePageOffsetClause(pivotId, pivotDirection, metadataSource, orderCriteria, sqlParamsSource, filterClauses);
        }
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
    public ResponseEntity<SubjectsResponse> getSubjectsV2(final String fields, final String sortBy, final String name, final FilterOperand nameOp, final String ticker, final FilterOperand tickerOp, final String description, final FilterOperand descriptionOp, final String url, final FilterOperand urlOp, final String policy, final FilterOperand policyOp, final LocalDate updated, final FilterOperand updatedOp, final String updatedBy, final FilterOperand updatedbyOp, final Integer decimals, final FilterOperand decimalsOp, final String q, final String vkey, final Integer limit, final Long page, final String pivotId, final PivotDirection pivotDirection, final String network) {
        try {
            final String metadataSource = offchainMetadataRegistryConfig.sourceFromNetwork(sanitizeNetworkRequestParameter(network));
            final PivotDirection pivotDirectionSanitized = (pivotId != null && pivotDirection == null) ? PivotDirection.AFTER : pivotDirection;
            final int pageSize = pageSizeFromLimitQueryParam(limit);
            final Map<String, Object> sqlParamsSourceTotalCount = new HashMap<>();
            final String filterClauseTotalCount = whereClauseFromQueryParams(name, nameOp, ticker, tickerOp, description, descriptionOp, url, urlOp, policy, policyOp, updated, updatedOp, updatedBy, updatedbyOp, decimals, decimalsOp, q, pivotId, pivotDirectionSanitized, metadataSource, sortBy, sqlParamsSourceTotalCount, true);
            final SqlParameterSource paramsTotalCountParameters = new MapSqlParameterSource(sqlParamsSourceTotalCount);
            final List<Long> totalResultSetCountQueryResult = namedParameterJdbcTemplate.query(String.format("SELECT count(*) as cnt FROM metadata %s", filterClauseTotalCount), paramsTotalCountParameters, (rs, rowNum) -> rs.getLong("cnt"));
            final long totalResultSetCount = totalResultSetCountQueryResult.isEmpty() ? 0 : totalResultSetCountQueryResult.get(0);
            final long pageSanitized = (page != null) ? Math.min(totalResultSetCount / pageSize, Math.max(page, 0)) : 0;
            final String orderByClause = orderClauseFromQueryParam(sortBy, pivotId != null && pivotDirectionSanitized == PivotDirection.BEFORE);
            final Map<String, Object> sqlParamsSource = new HashMap<>();
            final String filterClause = whereClauseFromQueryParams(name, nameOp, ticker, tickerOp, description, descriptionOp, url, urlOp, policy, policyOp, updated, updatedOp, updatedBy, updatedbyOp, decimals, decimalsOp, q, pivotId, pivotDirectionSanitized, metadataSource, sortBy, sqlParamsSource, false);
            final SqlParameterSource params = new MapSqlParameterSource(sqlParamsSource);
            final List<MetadataQueryResult> queryResults = namedParameterJdbcTemplate.query(String.format("SELECT * FROM metadata %s ORDER BY %s LIMIT %d OFFSET %d", filterClause, orderByClause, pageSize, pageSanitized * pageSize), params, (rs, rowNum) -> MetadataQueryResult.fromSqlResultSet(rs));
            if (queryResults.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            } else {
                final List<String> fieldsToExclude = (fields == null) ? new ArrayList<>() : new ArrayList<>(MetadataQueryResult.DEFAULT_PROPERTY_NAMES);
                if (fields != null && !fields.isBlank()) {
                    fieldsToExclude.removeAll(List.of(fields.split(",")));
                }
                final List<TokenMetadata> properties = new ArrayList<>();
                for (final MetadataQueryResult metadataQueryResult : queryResults) {
                    properties.add(metadataQueryResult.toTokenMetadata(fieldsToExclude));
                }

                // reverse the order for backwards pagination
                if (pivotId != null && pivotDirectionSanitized == PivotDirection.BEFORE) {
                    Collections.reverse(properties);
                }

                final SubjectsResponse subjectsResponse = new SubjectsResponse();
                subjectsResponse.setSubjects(properties);

                final HttpHeaders headers = new HttpHeaders();
                headers.add("X-Total-Count", String.valueOf(totalResultSetCount));
                return new ResponseEntity<>(subjectsResponse, headers, HttpStatus.OK);
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
    public ResponseEntity<TokenMetadata> postSubjectV2(final String subject, final TokenMetadata property, final String network) {
        // 1. verfiy data
        // 2. submit as PR to Github or where-ever
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @Override
    public ResponseEntity<TokenMetadata> postSignaturesV2(final String subject, final TokenMetadata property, final String network) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    private boolean propertyHasRequiredFields(final TokenMetadata property) {
        return property.getSubject() != null && !property.getSubject().isBlank() && property.getName() != null && property.getName().getValue() != null && !property.getName().getValue().isBlank() && property.getDescription() != null && property.getDescription().getValue() != null && !property.getDescription().getValue().isBlank();
    }

    @Override
    public ResponseEntity<Void> verifySubjectV2(final String subject, final TokenMetadata property, final String network) {
        // property has required fields
        if (!propertyHasRequiredFields(property)) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        // subject must match subject in property
        if (!property.getSubject().equals(subject)) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        // 1. first bytes of subject should match the policyId (if any)
        if (property.getPolicy() != null && !property.getSubject().startsWith(property.getPolicy())) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        // 3. ticker name is not too long
        if (property.getTicker() != null && !property.getTicker().getValue().isEmpty() && (property.getTicker().getValue().length() < 2 || property.getTicker().getValue().length() > 9)) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        // 3. name is not too long
        // 4. description is not too long
        // 5. logo size is not too big
        // 6. decimals is a sane value (>= 0 < X)
        // 7. url makes sense
        // 8. validate given signatures
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @Override
    public ResponseEntity<TokenMetadata> deleteSubjectV2(final String subject, final String signature, final String vkey, final String network) {
        // 1. verify signature (which is sig(subject | "VOID")) with given vkey
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    private static List<String> sanitizeAddressHashes(@NotNull final List<String> walletAddressHashes) throws IllegalArgumentException {
        return walletAddressHashes.stream().map(walletAddressHash -> {
            if (walletAddressHash.trim().isEmpty()) {
                throw new IllegalArgumentException("Wallet address hash cannot be empty.");
            } else {
                final Matcher matcher = HEXADECIMAL_PATTERN.matcher(walletAddressHash.trim());
                if (!matcher.matches() || walletAddressHash.trim().length() >= 64) {
                    throw new IllegalArgumentException("Wallet address hash is not a valid hex represented SHA-256 hash.");
                } else {
                    return walletAddressHash.trim().toLowerCase(Locale.ROOT);
                }
            }
        }).collect(Collectors.toList());
    }

    @Override
    public ResponseEntity<WalletTrustCheckResponse> v2ForensicsWallet(final String walletAddressHash) {
        return v2ForensicsWallets(new WalletHashes(List.of(walletAddressHash)));
    }

    @Override
    public ResponseEntity<WalletTrustCheckResponse> v2ForensicsWallets(final WalletHashes walletAddressHashes) {
        try {
            final List<WalletFraudIncident> fraudIncidents = extendedMetadataIndexer.findScamIncidents(sanitizeAddressHashes(walletAddressHashes.getAddressHashes()));
            if (fraudIncidents.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            } else {
                final WalletTrustCheckResponse response = new WalletTrustCheckResponse();
                response.setIncidents(fraudIncidents);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
        } catch (final IllegalArgumentException e) {
            log.error("Could not process request due to invalid arguments provided.", e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (final IllegalStateException e) {
            log.error("Could not process request due to an internal server error.", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
