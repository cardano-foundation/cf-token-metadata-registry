package org.cardanofoundation.tokenmetadata.registry.it;

import com.bloxbean.cardano.client.backend.blockfrost.service.BFBackendService;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.cardanofoundation.tokenmetadata.registry.it.Cip68TestMinter.FT_PREFIX;
import static org.cardanofoundation.tokenmetadata.registry.it.Cip68TestMinter.NFT_PREFIX;
import static org.cardanofoundation.tokenmetadata.registry.it.Cip68TestMinter.RFT_PREFIX;

/**
 * Validation errors are reported, not enforced (ADR-019), end to end: CIP-68 tokens that break CIP-68 in known ways are
 * minted on the devnet, indexed as written and served by V2 with their {@code validation_errors}; {@code ?validation=}
 * and {@code ?property=} decide what a client gets.
 */
public class ValidationErrorsIntegrationIT extends BaseIntegrationIT {

    private static final Cip68TestMinter.Cip68Token VALID = new Cip68TestMinter.Cip68Token("VEValid", List.of(FT_PREFIX),
            Map.of("name", "Valid FT", "description", "Follows CIP-68", "ticker", "VALID", "logo", "ipfs://bafyvalidlogo"), 6L, 1);
    private static final Cip68TestMinter.Cip68Token NO_DESCRIPTION = new Cip68TestMinter.Cip68Token("VENoDesc", List.of(FT_PREFIX),
            Map.of("name", "No Description FT", "ticker", "NODESC"), 6L, 1);
    private static final Cip68TestMinter.Cip68Token BARE_CID_LOGO = new Cip68TestMinter.Cip68Token("VEBareLogo", List.of(FT_PREFIX),
            Map.of("name", "Bare CID Logo FT", "description", "Logo is a bare IPFS CID", "ticker", "BARE",
                    "logo", "QmQcx87EPNygLxz7p4ZqKU7CodSoetA2LFQ3UFLhMWtrtk"), 6L, 1);
    private static final Cip68TestMinter.Cip68Token VERSION_0 = new Cip68TestMinter.Cip68Token("VEVersion0", List.of(FT_PREFIX),
            Map.of("name", "Version 0 FT", "description", "Declares version 0", "ticker", "VZERO"), 6L, 0);
    private static final Cip68TestMinter.Cip68Token RICH_FT = new Cip68TestMinter.Cip68Token("VERichFT", List.of(RFT_PREFIX),
            Map.of("name", "Rich FT", "image", "ipfs://bafyrichimage"), 2L, 1);
    private static final Cip68TestMinter.Cip68Token LP_WITH_POOL_NFT = new Cip68TestMinter.Cip68Token("VEPoolLP",
            List.of(NFT_PREFIX, FT_PREFIX), Map.of("name", "Pool LP", "description", "LP token minted with a pool NFT", "ticker", "LP"), 6L, 1);

    private static final List<Cip68TestMinter.Cip68Token> TOKENS =
            List.of(VALID, NO_DESCRIPTION, BARE_CID_LOGO, VERSION_0, RICH_FT, LP_WITH_POOL_NFT);

    @BeforeAll
    static void setUp() throws Exception {
        waitForApiReady();
        waitForYaciStoreReady();

        Cip68TestMinter minter = new Cip68TestMinter(new BFBackendService(YACI_STORE_URL, "Dummy"), restTemplate);
        for (Cip68TestMinter.Cip68Token token : TOKENS) {
            minter.mintCip68Token(token, 1_000_000);
        }
        for (Cip68TestMinter.Cip68Token token : TOKENS) {
            waitForIndexed(servedSubject(token));
        }
    }

    /** The subject the V2 API serves for a token: its 444 subject for an RFT, its 333 subject otherwise. */
    private static String servedSubject(Cip68TestMinter.Cip68Token token) {
        return token.subject(token.userTokenPrefixes().contains(RFT_PREFIX) ? RFT_PREFIX : FT_PREFIX);
    }

    private static void waitForIndexed(String subject) {
        await().atMost(Duration.ofMinutes(5))
                .pollInterval(Duration.ofSeconds(2))
                .ignoreExceptions()
                .until(() -> {
                    ResponseEntity<String> response = restTemplate.getForEntity(API_BASE_URL + "/api/v2/subjects/" + subject, String.class);
                    return response.getStatusCode() == HttpStatus.OK
                            && "CIP_68".equals(JsonPath.parse(response.getBody()).read("$.subject.metadata.name.source", String.class));
                });
    }

    private static DocumentContext get(String subject, String query) {
        ResponseEntity<String> response = restTemplate.getForEntity(
                API_BASE_URL + "/api/v2/subjects/" + subject + (query.isEmpty() ? "" : "?" + query), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return JsonPath.parse(response.getBody());
    }

    private static HttpStatusCode status(String subject, String query) {
        try {
            return restTemplate.getForEntity(API_BASE_URL + "/api/v2/subjects/" + subject + "?" + query, String.class).getStatusCode();
        } catch (HttpStatusCodeException e) {
            return e.getStatusCode();
        }
    }

    private static List<Map<String, Object>> errors(DocumentContext json) {
        return json.read("$.subject.validation_errors");
    }

    private static List<String> subjectsOf(String validation) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String subjects = String.join("\",\"", TOKENS.stream().map(ValidationErrorsIntegrationIT::servedSubject).toList());
        ResponseEntity<String> response = restTemplate.postForEntity(
                API_BASE_URL + "/api/v2/subjects/query?validation=" + validation,
                new HttpEntity<>("{\"subjects\": [\"" + subjects + "\"], \"properties\": []}", headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return JsonPath.parse(response.getBody()).read("$.subjects[*].subject");
    }

    @Nested
    @DisplayName("A token that follows CIP-68")
    class ValidToken {

        @Test
        void hasNoErrorsAndIsValid() {
            DocumentContext json = get(servedSubject(VALID), "");

            assertThat(errors(json)).isEmpty();
            assertThat(json.read("$.subject.metadata.valid", Boolean.class)).isTrue();
            assertThat(status(servedSubject(VALID), "validation=STRICT")).isEqualTo(HttpStatus.OK);
        }
    }

    @Nested
    @DisplayName("A token that breaks CIP-68 is served with its errors")
    class TokensWithErrors {

        @Test
        void aFungibleTokenWithoutDescriptionHasAMandatoryError() {
            DocumentContext json = get(servedSubject(NO_DESCRIPTION), "");

            assertThat(json.read("$.subject.metadata.name.value", String.class)).isEqualTo("No Description FT");
            assertThat(json.read("$.subject.metadata.valid", Boolean.class)).isFalse();
            assertThat(errors(json)).anySatisfy(error -> assertThat(error)
                    .containsEntry("cip", "CIP-68").containsEntry("sub_standard", "333")
                    .containsEntry("field_name", "description").containsEntry("field_category", "MANDATORY")
                    .containsEntry("code", "missing_value"));
        }

        @Test
        void aBareCidLogoIsKeptAsWrittenWithAnOptionalError() {
            DocumentContext json = get(servedSubject(BARE_CID_LOGO), "");

            assertThat(json.read("$.subject.metadata.logo.value", String.class)).isEqualTo("QmQcx87EPNygLxz7p4ZqKU7CodSoetA2LFQ3UFLhMWtrtk");
            assertThat(errors(json)).singleElement().satisfies(error -> assertThat(error)
                    .containsEntry("field_name", "logo").containsEntry("field_category", "OPTIONAL")
                    .containsEntry("code", "invalid_uri_scheme").doesNotContainKey("sub_standard"));
        }

        @Test
        void aVersionCip68DoesNotDefineIsKeptWithAMandatoryError() {
            DocumentContext json = get(servedSubject(VERSION_0), "");

            assertThat(json.read("$.subject.metadata.version.value", Integer.class)).isZero();
            assertThat(errors(json)).anySatisfy(error -> assertThat(error)
                    .containsEntry("field_name", "version").containsEntry("code", "undefined_version")
                    .containsEntry("field_category", "MANDATORY"));
        }
    }

    @Nested
    @DisplayName("Labels")
    class Labels {

        @Test
        void aRichFungibleTokenIsServedWithItsImageAsTheLogoAndNeedsNoDescription() {
            DocumentContext json = get(servedSubject(RICH_FT), "");

            assertThat(json.read("$.subject.metadata.logo.value", String.class)).isEqualTo("ipfs://bafyrichimage");
            assertThat(json.read("$.subject.metadata.decimals.value", Integer.class)).isEqualTo(2);
            assertThat(errors(json)).isEmpty();
            assertThat(json.read("$.subject.metadata.valid", Boolean.class)).isTrue();
        }

        @Test
        void aFungibleTokenMintedWithAnNftIsServedWithoutTheNftRequirement() {
            // the SundaeSwap LP case: the 222 pool NFT requires an image, the 333 LP token does not
            DocumentContext json = get(servedSubject(LP_WITH_POOL_NFT), "");

            assertThat(json.read("$.subject.metadata.name.value", String.class)).isEqualTo("Pool LP");
            assertThat(errors(json)).isEmpty();
            assertThat(status(LP_WITH_POOL_NFT.subject(NFT_PREFIX), "")).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    /** A 333 or 444 subject is served only when its user token was minted (ADR-022). */
    @Nested
    @DisplayName("Only minted user tokens are served")
    class MintedUserTokensOnly {

        @Test
        void theRichFungibleSubjectOfAFungibleTokenIsNotServed() {
            assertThat(status(VALID.subject(FT_PREFIX), "")).isEqualTo(HttpStatus.OK);
            assertThat(status(VALID.subject(RFT_PREFIX), "")).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        void theFungibleSubjectOfARichFungibleTokenIsNotServed() {
            assertThat(status(RICH_FT.subject(RFT_PREFIX), "")).isEqualTo(HttpStatus.OK);
            assertThat(status(RICH_FT.subject(FT_PREFIX), "")).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        void aBatchLeavesOutSubjectsThatWereNeverMinted() {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String body = "{\"subjects\": [\"" + VALID.subject(FT_PREFIX) + "\", \"" + VALID.subject(RFT_PREFIX) + "\"], \"properties\": []}";
            ResponseEntity<String> response = restTemplate.postForEntity(API_BASE_URL + "/api/v2/subjects/query",
                    new HttpEntity<>(body, headers), String.class);

            List<String> subjects = JsonPath.parse(response.getBody()).read("$.subjects[*].subject");
            assertThat(subjects).containsExactly(VALID.subject(FT_PREFIX));
        }
    }

    /** ?validation= chooses which tokens a client gets; the errors of a returned token are not filtered. */
    @Nested
    @DisplayName("?validation=")
    class ValidationScope {

        @Test
        void mandatoryOnlyHidesTokensWithAMandatoryError() {
            assertThat(status(servedSubject(NO_DESCRIPTION), "validation=MANDATORY_ONLY")).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(status(servedSubject(VERSION_0), "validation=MANDATORY_ONLY")).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(status(servedSubject(BARE_CID_LOGO), "validation=MANDATORY_ONLY")).isEqualTo(HttpStatus.OK);
        }

        @Test
        void strictHidesTokensWithAnyError() {
            assertThat(status(servedSubject(BARE_CID_LOGO), "validation=STRICT")).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(status(servedSubject(RICH_FT), "validation=STRICT")).isEqualTo(HttpStatus.OK);
        }

        @Test
        void aBatchLeavesOutTheTokensTheScopeDoesNotAccept() {
            assertThat(subjectsOf("ALL")).hasSize(TOKENS.size());
            assertThat(subjectsOf("MANDATORY_ONLY")).containsExactlyInAnyOrder(
                    servedSubject(VALID), servedSubject(BARE_CID_LOGO), servedSubject(RICH_FT), servedSubject(LP_WITH_POOL_NFT));
            assertThat(subjectsOf("STRICT")).containsExactlyInAnyOrder(
                    servedSubject(VALID), servedSubject(RICH_FT), servedSubject(LP_WITH_POOL_NFT));
        }

        @Test
        void underAPropertyFilterTheErrorsAreAboutThoseProperties() {
            // the missing description is not an error of a client that asks for the ticker
            DocumentContext json = get(servedSubject(NO_DESCRIPTION), "property=ticker&validation=STRICT");

            assertThat(json.read("$.subject.metadata.ticker.value", String.class)).isEqualTo("NODESC");
            assertThat(errors(json)).isEmpty();
            assertThat(json.read("$.subject.metadata.valid", Boolean.class)).isTrue();
        }
    }
}
