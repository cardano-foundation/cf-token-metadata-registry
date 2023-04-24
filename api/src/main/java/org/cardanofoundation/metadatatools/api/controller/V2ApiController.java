package org.cardanofoundation.metadatatools.api.controller;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import jakarta.validation.constraints.NotNull;
import lombok.extern.log4j.Log4j2;
import org.cardanofoundation.metadatatools.api.config.OffchainMetadataRegistryConfig;
import org.cardanofoundation.metadatatools.api.indexer.FetchMetadataResultSet;
import org.cardanofoundation.metadatatools.api.indexer.V1ApiMetadataIndexer;
import org.cardanofoundation.metadatatools.api.indexer.V2ApiMetadataIndexer;
import org.cardanofoundation.metadatatools.api.model.rest.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;

@Log4j2
@Controller
@CrossOrigin(exposedHeaders = {"X-Total-Count"})
@RequestMapping("${openapi.metadataServer.base-path:}")
public class V2ApiController implements V2Api {

  private static final int DEFAULT_PAGE_SIZE = 20;
  private static final int MIN_PAGE_SIZE = 1;
  private static final int MAX_PAGE_SIZE = 200;
  private static final Pattern HEXADECIMAL_PATTERN = Pattern.compile("\\p{XDigit}+");

  @Autowired private OffchainMetadataRegistryConfig offchainMetadataRegistryConfig;

  @Autowired private V1ApiMetadataIndexer v1ApiMetadataIndexer;

  @Autowired private V2ApiMetadataIndexer v2ApiMetadataIndexer;

  private static String sanitizeNetworkRequestParameter(final String network) {
    return network.trim().toLowerCase(Locale.ROOT);
  }

  @Override
  public ResponseEntity<TokenMetadata> getSubjectV2(
      final String network, final String subject, final String fields) {
    try {
      final Optional<TokenMetadata> queryResult;
      if (fields != null) {
        queryResult =
            v1ApiMetadataIndexer.findSubjectSelectProperties(
                offchainMetadataRegistryConfig.sourceFromNetwork(
                    sanitizeNetworkRequestParameter(network)),
                subject,
                List.of(fields.trim().split(",")));
      } else {
        queryResult =
            v1ApiMetadataIndexer.findSubject(
                offchainMetadataRegistryConfig.sourceFromNetwork(
                    sanitizeNetworkRequestParameter(network)),
                subject);
      }
      return queryResult
          .map(tokenMetadata -> new ResponseEntity<>(tokenMetadata, HttpStatus.OK))
          .orElseGet(() -> new ResponseEntity<>(HttpStatus.NO_CONTENT));
    } catch (final IllegalArgumentException e) {
      log.error("Not able to process request for single subject.", e);
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    } catch (final IllegalStateException e) {
      log.error("Could not process query result.", e);
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  private static int pageSizeFromLimitQueryParam(final Integer limit) {
    return (limit == null)
        ? DEFAULT_PAGE_SIZE
        : Math.max(MIN_PAGE_SIZE, Math.min(limit, MAX_PAGE_SIZE));
  }

  @Override
  public ResponseEntity<SubjectsResponse> getSubjectsV2(
      final String network,
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
      final String metadataSource =
          offchainMetadataRegistryConfig.sourceFromNetwork(
              sanitizeNetworkRequestParameter(network));
      final int pageSize = pageSizeFromLimitQueryParam(limit);
      final PivotDirection pivotDirectionSanitized =
          (pivotId != null && pivotDirection == null) ? PivotDirection.AFTER : pivotDirection;

      final FetchMetadataResultSet resultSet =
          v2ApiMetadataIndexer.fetchMetadata(
              fields,
              sortBy,
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
              vkey,
              pageSize,
              page,
              pivotId,
              pivotDirectionSanitized,
              metadataSource);
      if (resultSet.getTotalCount() == 0) {
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
      } else {
        final SubjectsResponse subjectsResponse = new SubjectsResponse();
        subjectsResponse.setSubjects(resultSet.getResults());

        final HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", String.valueOf(resultSet.getTotalCount()));
        return new ResponseEntity<>(subjectsResponse, headers, HttpStatus.OK);
      }
    } catch (final IllegalArgumentException e) {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    } catch (final IllegalStateException e) {
      log.error("Could not process query result.", e);
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public ResponseEntity<TokenMetadata> postSubjectV2(
      @NotNull final String network, final String subject, final TokenMetadata property) {
    // 1. verfiy data
    // 2. submit as PR to Github or where-ever
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  @Override
  public ResponseEntity<TokenMetadata> postSignaturesV2(
      @NotNull final String network, final String subject, final TokenMetadata property) {
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
  public ResponseEntity<Void> verifySubjectV2(
      @NotNull final String network, final String subject, final TokenMetadata property) {
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
    if (property.getTicker() != null
        && !property.getTicker().getValue().isEmpty()
        && (property.getTicker().getValue().length() < 2
            || property.getTicker().getValue().length() > 9)) {
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
  public ResponseEntity<TokenMetadata> deleteSubjectV2(
      @NotNull final String network,
      @NotNull final String subject,
      @NotNull final String signature,
      @NotNull final String vkey) {
    // 1. verify signature (which is sig(subject | "VOID")) with given vkey
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  private static List<String> sanitizeAddressHashes(@NotNull final List<String> walletAddressHashes)
      throws IllegalArgumentException {
    return walletAddressHashes.stream()
        .map(
            walletAddressHash -> {
              if (walletAddressHash.trim().isEmpty()) {
                throw new IllegalArgumentException("Wallet address hash cannot be empty.");
              } else {
                final Matcher matcher = HEXADECIMAL_PATTERN.matcher(walletAddressHash.trim());
                if (!matcher.matches() || walletAddressHash.trim().length() >= 64) {
                  throw new IllegalArgumentException(
                      "Wallet address hash is not a valid hex represented SHA-256 hash.");
                } else {
                  return walletAddressHash.trim().toLowerCase(Locale.ROOT);
                }
              }
            })
        .collect(Collectors.toList());
  }

  @Override
  public ResponseEntity<WalletTrustCheckResponse> v2ForensicsWallet(
      final String walletAddressHash) {
    return v2ForensicsWallets(new WalletHashes(List.of(walletAddressHash)));
  }

  @Override
  public ResponseEntity<WalletTrustCheckResponse> v2ForensicsWallets(
      final WalletHashes walletAddressHashes) {
    try {
      final List<WalletFraudIncident> fraudIncidents =
          v2ApiMetadataIndexer.findScamIncidents(
              sanitizeAddressHashes(walletAddressHashes.getAddressHashes()));
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
