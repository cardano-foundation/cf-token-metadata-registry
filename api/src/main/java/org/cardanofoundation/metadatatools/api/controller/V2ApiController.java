package org.cardanofoundation.metadatatools.api.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.cardanofoundation.metadatatools.api.model.data.MetadataQueryResult;
import org.cardanofoundation.metadatatools.api.model.data.SubmitToken;
import org.cardanofoundation.metadatatools.api.model.data.WalletScamLookupQueryResult;
import org.cardanofoundation.metadatatools.api.model.rest.*;
import org.cardanofoundation.metadatatools.api.repository.SubmitTokenRepo;
import org.cardanofoundation.metadatatools.core.TokenMetadataCreator;
import org.cardanofoundation.metadatatools.core.ValidationResult;
import org.cardanofoundation.metadatatools.core.crypto.keys.Key;
import org.cardanofoundation.metadatatools.core.crypto.keys.KeyType;
import org.cardanofoundation.metadatatools.core.model.KeyTextEnvelope;
import org.cardanofoundation.metadatatools.core.model.PolicyScript;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import static java.util.Map.entry;

@Log4j2
@RestController
@CrossOrigin(exposedHeaders={"X-Total-Count"})
@RequestMapping("${openapi.metadataServer.base-path:}")
public class V2ApiController implements V2Api {


    @Value("${git.fork.repository.path}")
    private String gitForkRepoPath;
    @Value("${git.personal.token}")
    private String gitPersonalToken;
    @Value("${git.username}")
    private String gitUsername;
    @Value("${git.main.branch}")
    private String gitMainBranch;
    @Value("${git.cardano.repository.url}")
    private String gitCardanoRepoUrl;

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private SubmitTokenRepo submitTokenRepo;

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 200;
    private static final String DEFAULT_ORDER_CLAUSE = "subject ASC";
    private static final String DEFAULT_ORDER_CLAUSE_FLIPPED = "subject DESC";
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

    private final ObjectMapper mapper = new ObjectMapper();
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
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<TokenMetadata> getSubjectV2(final String subject, final String fields) {
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
                final TokenMetadata returnProperty;
                if (fields != null) {
                    final List<String> fieldsToExclude = new ArrayList<>(MetadataQueryResult.DEFAULT_PROPERTY_NAMES);
                    fieldsToExclude.removeAll(List.of(fields.split(",")));
                    returnProperty = queryResults.get(0).toTokenMetadata(fieldsToExclude);
                } else {
                    returnProperty = queryResults.get(0).toTokenMetadata();
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
                    : String.format("%s %s, %s", columnNameFromCriteriaName(sortCriteria), direction, flipOrdering ? DEFAULT_ORDER_CLAUSE_FLIPPED : DEFAULT_ORDER_CLAUSE))
                    : flipOrdering ? DEFAULT_ORDER_CLAUSE_FLIPPED : DEFAULT_ORDER_CLAUSE;
        }
    }

    private static String columnNameFromCriteriaName(@NotNull final String sortCriteria) {
        return CRITERIA_COLUMN_NAME_MAPPING.getOrDefault(sortCriteria, DEFAULT_CRITERIA_COLUMN_NAME);
    }

    private static int pageSizeFromLimitQueryParam(final Integer limit) {
        return (limit == null) ? DEFAULT_PAGE_SIZE : Math.max(MIN_PAGE_SIZE, Math.min(limit, MAX_PAGE_SIZE));
    }

    private void computePageOffsetClause(final String pivotId,
                                         final PivotDirection pivotDirection,
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
                                              final String pivotId,
                                              final PivotDirection pivotDirection,
                                              final String orderCriteria,
                                              final Map<String, Object> sqlParamsSource,
                                              final boolean noPageOffsetClause) {
        final List<String> filterClauses = new ArrayList<>();
        if (!noPageOffsetClause) {
            computePageOffsetClause(pivotId, pivotDirection, orderCriteria, sqlParamsSource, filterClauses);
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
    public ResponseEntity<SubjectsResponse> getSubjectsV2(
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
            final Integer limit,
            final Long page,
            final String pivotId,
            final PivotDirection pivotDirection) {
        try {
            final PivotDirection pivotDirectionSanitized = (pivotId != null && pivotDirection == null) ? PivotDirection.AFTER : pivotDirection;
            final int pageSize = pageSizeFromLimitQueryParam(limit);
            final Map<String, Object> sqlParamsSourceTotalCount = new HashMap<>();
            final String filterClauseTotalCount = whereClauseFromQueryParams(
                    name, nameOp, ticker, tickerOp, description, descriptionOp, url, urlOp, policy, policyOp, updated,
                    updatedOp, updatedBy, updatedbyOp, decimals, decimalsOp, q, pivotId, pivotDirectionSanitized, sortBy, sqlParamsSourceTotalCount, true);
            final SqlParameterSource paramsTotalCountParameters = new MapSqlParameterSource(sqlParamsSourceTotalCount);
            final List<Long> totalResultSetCountQueryResult = namedParameterJdbcTemplate.query(String.format("SELECT count(*) as cnt FROM metadata %s", filterClauseTotalCount), paramsTotalCountParameters, (rs, rowNum) -> rs.getLong("cnt"));
            final long totalResultSetCount = totalResultSetCountQueryResult.isEmpty() ? 0 : totalResultSetCountQueryResult.get(0);
            final long pageSanitized = (page != null) ? Math.min(totalResultSetCount / pageSize, Math.max(page, 0)) : 0;
            final String orderByClause = orderClauseFromQueryParam(sortBy, pivotId != null && pivotDirectionSanitized == PivotDirection.BEFORE);
            final Map<String, Object> sqlParamsSource = new HashMap<>();
            final String filterClause = whereClauseFromQueryParams(
                    name, nameOp, ticker, tickerOp, description, descriptionOp, url, urlOp, policy, policyOp, updated,
                    updatedOp, updatedBy, updatedbyOp, decimals, decimalsOp, q, pivotId, pivotDirectionSanitized, sortBy, sqlParamsSource, false);
            final SqlParameterSource params = new MapSqlParameterSource(sqlParamsSource);
            final List<MetadataQueryResult> queryResults = namedParameterJdbcTemplate.query(String.format("SELECT * FROM metadata %s ORDER BY %s LIMIT %d OFFSET %d", filterClause, orderByClause, pageSize, pageSanitized * pageSize), params, (rs, rowNum) -> MetadataQueryResult.fromSqlResultSet(rs));
            if (queryResults.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            } else {
                final List<String> fieldsToExclude = (fields == null)
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
    public ResponseEntity<List<SubmitToken>> getSubjectsByWallet(String updatedBy) {
        try {
            return ResponseEntity.status(HttpStatus.OK).body(submitTokenRepo.findAllByUpdatedBy(updatedBy));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<?> postSubjectV2(SubmitToken submitToken) throws IOException {
        // 1. verfiy data
        PolicyScript policyScript;
        if (submitToken.getPolicyScript() != null) {
            try {
                policyScript = mapper.readValue(submitToken.getPolicyScript().getInputStream(), PolicyScript.class);
            } catch (IOException exception) {
                exception.printStackTrace();
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Policy.script invalid format");
            }
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Policy.script are required!");
        }
        org.cardanofoundation.metadatatools.core.model.TokenMetadata tokenMetadataSubmit = new org.cardanofoundation.metadatatools.core.model.TokenMetadata(submitToken.getAssetName() == null ? "" : submitToken.getAssetName(), policyScript);
        if(submitToken.getName() != null) {
            tokenMetadataSubmit.addProperty("name", new org.cardanofoundation.metadatatools.core.model.TokenMetadataProperty<>(submitToken.getName(), 0, null));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Name are required!");
        }
        if(submitToken.getDescription() != null) {
            tokenMetadataSubmit.addProperty("description", new org.cardanofoundation.metadatatools.core.model.TokenMetadataProperty<>(submitToken.getDescription(), 0, null));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Description are required!");
        }
        if(submitToken.getDecimals() != null && submitToken.getDecimals() >= 0 && submitToken.getDecimals() <= 255) {
            tokenMetadataSubmit.addProperty("decimals", new org.cardanofoundation.metadatatools.core.model.TokenMetadataProperty<>(submitToken.getDecimals(), 0, null));
        } else if (submitToken.getDecimals() != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Decimals must be between 0 and 255!");
        }
        if(submitToken.getUrl() != null && !submitToken.getUrl().isEmpty() && submitToken.getUrl().matches("^https://.*")) {
            tokenMetadataSubmit.addProperty("url", new org.cardanofoundation.metadatatools.core.model.TokenMetadataProperty<>(submitToken.getUrl(), 0, null));
        } else if (submitToken.getUrl() != null && !submitToken.getUrl().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Url must be start with https://");
        }
        if(submitToken.getTicker() != null && submitToken.getTicker().length() >= 2 && submitToken.getTicker().length() <= 9) {
            tokenMetadataSubmit.addProperty("ticker", new org.cardanofoundation.metadatatools.core.model.TokenMetadataProperty<>(submitToken.getTicker(), 0, null));
        } else if (submitToken.getTicker() != null && !submitToken.getTicker().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Ticker's length must be >= 2 and <= 9 characters");
        }
        if(!submitToken.getLogo().isEmpty() && submitToken.getLogo() != null) {
            tokenMetadataSubmit.addProperty("logo", new org.cardanofoundation.metadatatools.core.model.TokenMetadataProperty<>(submitToken.getLogo(), 0, null));
        }
        KeyTextEnvelope signingEnvelope;
        if (submitToken.getPolicySkey() != null) {
            try {
                signingEnvelope = mapper.readValue(submitToken.getPolicySkey().getInputStream(), KeyTextEnvelope.class);
            } catch (IOException exception) {
                exception.printStackTrace();
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Policy.skey invalid format");
            }
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Policy.skey are required!");
        }
        Key signingKey = Key.fromTextEnvelope(signingEnvelope, KeyType.POLICY_SIGNING_KEY_ED25519);
        TokenMetadataCreator.signTokenMetadata(tokenMetadataSubmit, signingKey);
        // Verify tokenMetadata
        Key verificationKey = signingKey.generateVerificationKey();
        ValidationResult validationResult = TokenMetadataCreator.validateTokenMetadata(tokenMetadataSubmit, verificationKey);
        if(!validationResult.isValid() && validationResult.getValidationErrors().size() > 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(validationResult.getValidationErrors().get(0));
        };
        // 2. submit as PR to Github or where-ever
        // 2.1 Fork and clone Cardano Token Registry Repository to github account
        // 2.1.1 First we need have to Fork Cardano Token Registry Repository to a github account, maybe set in config
        // 2.1.2 Clone repo to specific config folder if it is first time
        File localRepoDir = new File(gitForkRepoPath + "/mappings");
        // Repository must be cloned before
        if(localRepoDir.exists()) {
            log.info("Exist local repo");
            try {
                // validate metadata
                // 2.2 Create json file and move to mappings folder
                //save new file to mappings folder
                StringBuilder filePath = new StringBuilder(gitForkRepoPath + "/mappings/" + tokenMetadataSubmit.getSubject() + ".json");
                mapper.writerWithDefaultPrettyPrinter().writeValue(new File(filePath.toString()), tokenMetadataSubmit);
                // Push to account github repository
                log.info("Starting push new file to origin remote");
                Git git = Git.open(new File(gitForkRepoPath));
                // Repository exsistRepo = git.getRepository();
                Status status = git.status().call();
                for (String s : status.getUncommittedChanges()){
                    log.info("Deleted file = " + s);
                    git.rm().addFilepattern(s).call();
                }
                git.add().addFilepattern("mappings/" + tokenMetadataSubmit.getSubject() + ".json").call();
                git.commit().setMessage("Add new metadata json file: " + tokenMetadataSubmit.getSubject()).call();
                TextProgressMonitor consoleProgressMonitor = new TextProgressMonitor(new PrintWriter(System.out));
                Iterable<PushResult> pushResults = git.push().setProgressMonitor(consoleProgressMonitor).setCredentialsProvider(new UsernamePasswordCredentialsProvider(gitUsername, gitPersonalToken)).setRemote("origin").add(gitMainBranch).call();
                boolean pushFailed = false;
                for (final PushResult pushResult : pushResults) {
                    for (RemoteRefUpdate refUpdate : pushResult.getRemoteUpdates()) {
                        if (refUpdate.getStatus() != RemoteRefUpdate.Status.OK) {
                            // Push was rejected
                            log.error("Push failed!" , pushResult.getMessages());
                            pushFailed = true;
                        }
                    }
                }
                if(pushFailed) {
                    log.error("Pushing failed!");
                    return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                }
                log.info("Pushing done!");
                // 2.3 Using github account to create personal token and use for create pull request
                // Create pull request to merge repository with cardano foundation registry
                HttpHeaders headers = new HttpHeaders();
                headers.setAccept(Collections.singletonList( new MediaType("application", "vnd.github+json")));
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.add("Authorization", "token " + gitPersonalToken);
                // Save submitted token
                submitToken.setSubject(tokenMetadataSubmit.getSubject());
                submitToken.setPolicy(tokenMetadataSubmit.getPolicy());
                submitToken.setStatus("pending");
                submitToken.setUpdated(new Date());
                String tokenMetadataSubmitStr = mapper.writeValueAsString(tokenMetadataSubmit);
                submitToken.setProperties(mapper.readValue(tokenMetadataSubmitStr , TokenMetadata.class));
                try {
                    if(checkPullRequestExists(headers)) {
                        log.info("Pull request already exists!");
                        submitTokenRepo.save(submitToken);
                        return new ResponseEntity<>(submitToken.getSubject(), HttpStatus.OK);
                    }
                    if (createPullRequest(headers)) {
                        log.info("Create pull request successful!");
                        submitTokenRepo.save(submitToken);
                        return new ResponseEntity<>(submitToken.getSubject(), HttpStatus.OK);
                    }
                    log.error("Create pull request failed!");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (GitAPIException | IOException ex) {
                log.error("Failed!" , ex);
            }
        } else {
            log.error("Empty mappings folder from Git repository !");
        }
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<TokenMetadata> postSignaturesV2(String subject, TokenMetadata property) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    private boolean propertyHasRequiredFields(final TokenMetadata property) {
        return property.getSubject() != null
                && !property.getSubject().isBlank()
                && property.getName() != null
                && property.getName().getValue() != null
                && !property.getName().getValue().isBlank()
                && property.getDescription() != null
                && property.getDescription().getValue() != null
                && !property.getDescription().getValue().isBlank();
    }

    @Override
    public ResponseEntity<Void> verifySubjectV2(final String subject, final TokenMetadata property) {
        // apply validation rules

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
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @Override
    public ResponseEntity<TokenMetadata> deleteSubjectV2(String subject, String signature, String vkey) {
        // 1. verify signature (which is sig(subject | "VOID")) with given vkey
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @Override
    public ResponseEntity<?> deleteSubjectV2(SubmitToken submitToken) throws IOException {
        // 1. Check token exists by the wallet address - updated by
        SubmitToken submitTokenExists = submitTokenRepo.findBySubjectAndUpdatedBy(submitToken.getSubject(), submitToken.getUpdatedBy());
        // 1. Check token metadata json file in mappings folder
        StringBuilder filePath = new StringBuilder(gitForkRepoPath + "/mappings/" + submitToken.getSubject() + ".json");
        File jsonFile = new File(filePath.toString());
        if(!jsonFile.exists() || submitTokenExists == null){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Token did not exists !");
        }
        try {
            org.cardanofoundation.metadatatools.core.model.TokenMetadata tokenMetadataMapping = mapper.readValue(new File(filePath.toString()), org.cardanofoundation.metadatatools.core.model.TokenMetadata.class);
            // Check if token was deleted before
            if (tokenMetadataMapping.getProperties().get("name").equals("VOID") && tokenMetadataMapping.getProperties().get("description").equals("VOID")){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Token under delete processing !");
            }
            KeyTextEnvelope signingEnvelope;
            if (submitToken.getPolicySkey() != null) {
                try {
                    signingEnvelope = mapper.readValue(submitToken.getPolicySkey().getInputStream(), KeyTextEnvelope.class);
                } catch (IOException exception) {
                    exception.printStackTrace();
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Policy.skey invalid format");
                }
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Policy.skey are required!");
            }
            Key signingKey = Key.fromTextEnvelope(signingEnvelope, KeyType.POLICY_SIGNING_KEY_ED25519);
            String oldPublicKey = tokenMetadataMapping.getProperties().get("name").getSignatures().get(0).getPublicKey();
            tokenMetadataMapping.addProperty("name", new org.cardanofoundation.metadatatools.core.model.TokenMetadataProperty<>("VOID", tokenMetadataMapping.getProperties().get("name").getSequenceNumber() + 1, null));
            tokenMetadataMapping.addProperty("description", new org.cardanofoundation.metadatatools.core.model.TokenMetadataProperty<>("VOID", tokenMetadataMapping.getProperties().get("description").getSequenceNumber() + 1, null));
            TokenMetadataCreator.signTokenMetadata(tokenMetadataMapping, signingKey );
            String newPublicKey = tokenMetadataMapping.getProperties().get("name").getSignatures().get(0).getPublicKey();
            if(!oldPublicKey.equals(newPublicKey)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Incorrect policy.skey for this token!");
            }
            // Verify tokenMetadata
            Key verificationKey = signingKey.generateVerificationKey();
            ValidationResult validationResult = TokenMetadataCreator.validateTokenMetadata(tokenMetadataMapping, verificationKey);
            if(!validationResult.isValid() && validationResult.getValidationErrors().size() > 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(validationResult.getValidationErrors().get(0));
            };

            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(filePath.toString()), tokenMetadataMapping);
            log.info("Starting push new file to origin remote");
            Git git = Git.open(new File(gitForkRepoPath));
            git.add().addFilepattern("mappings/" + tokenMetadataMapping.getSubject() + ".json").call();
            git.commit().setMessage("Delete token by subject: " + tokenMetadataMapping.getSubject()).call();
            TextProgressMonitor consoleProgressMonitor = new TextProgressMonitor(new PrintWriter(System.out));
            Iterable<PushResult> pushResults = git.push().setProgressMonitor(consoleProgressMonitor).setCredentialsProvider(new UsernamePasswordCredentialsProvider(gitUsername, gitPersonalToken)).setRemote("origin").add(gitMainBranch).call();
            boolean pushFailed = false;
            for (final PushResult pushResult : pushResults) {
                for (RemoteRefUpdate refUpdate : pushResult.getRemoteUpdates()) {
                    if (refUpdate.getStatus() != RemoteRefUpdate.Status.OK) {
                        // Push was rejected
                        log.error("Push failed!" , pushResult.getMessages());
                        pushFailed = true;
                    }
                }
            }
            if(pushFailed) {
                log.error("Pushing failed!");
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
            log.info("Pushing done!");
            // Create pull request to merge repository with cardano foundation registry
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList( new MediaType("application", "vnd.github+json")));
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("Authorization", "token " + gitPersonalToken);
            try {
                // check pull request exists
                if(checkPullRequestExists(headers)) {
                    log.info("Pull request already exists!");
                    if (submitTokenExists.getStatus().equals("pending")) {
                        submitTokenRepo.delete(submitTokenExists);
                    }
                    return new ResponseEntity<>(submitToken.getSubject(), HttpStatus.OK);
                }
                if (createPullRequest(headers)) {
                    log.info("Create pull request successful!");
                    if (submitTokenExists.getStatus().equals("pending")) {
                        submitTokenRepo.delete(submitTokenExists);
                    }
                    return new ResponseEntity<>(submitToken.getSubject(), HttpStatus.OK);
                }
                log.error("Create pull request failed!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private boolean createPullRequest(HttpHeaders headers) throws JsonProcessingException {
        try {
            String pullBody = "{\"title\":\"Submit new changes for metadata\",\"body\":\"Please pull these awesome changes in!\",\"head\":\""+gitUsername+":master\",\"base\":\"master\"}";
            Object object = mapper.readValue(pullBody, Object.class);
            HttpEntity<Object> entity = new HttpEntity<>(object, headers);
            ResponseEntity<Object> response = restTemplate.exchange(gitCardanoRepoUrl + "/pulls", HttpMethod.POST, entity, Object.class);
            HttpStatus httpStatus = response.getStatusCode();
            if (httpStatus.value() == 201) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    };
    private boolean checkPullRequestExists(HttpHeaders headers) {
        ResponseEntity<Object> response = restTemplate.exchange(gitCardanoRepoUrl + "/pulls?state=open&head="+gitUsername+":master", HttpMethod.GET, new HttpEntity<>(headers), Object.class);
        if(((List<Object>) response.getBody()).size() > 0) {
            return true;
        }
        return false;
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
                final List<WalletFraudIncident> fraudIncidents = new ArrayList<>();
                for (final WalletScamLookupQueryResult scamLookupQueryResult : queryResults) {
                    final WalletFraudIncident fraudIncident = new WalletFraudIncident();
                    fraudIncident.setAddressHash(scamLookupQueryResult.getWalletHash());
                    fraudIncident.setIncidentId(scamLookupQueryResult.getScamIncidentId());
                    fraudIncident.setScamSiteDomain(scamLookupQueryResult.getDomain());
                    fraudIncident.setReportedDate(scamLookupQueryResult.getReported().toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate());
                    fraudIncidents.add(fraudIncident);
                }
                response.setIncidents(fraudIncidents);
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
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
