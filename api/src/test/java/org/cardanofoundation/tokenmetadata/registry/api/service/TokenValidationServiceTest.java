package org.cardanofoundation.tokenmetadata.registry.api.service;

import com.bloxbean.cardano.client.common.cbor.CborSerializationUtil;
import com.bloxbean.cardano.client.plutus.spec.BigIntPlutusData;
import com.bloxbean.cardano.client.plutus.spec.BytesPlutusData;
import com.bloxbean.cardano.client.plutus.spec.ConstrPlutusData;
import com.bloxbean.cardano.client.plutus.spec.PlutusData;
import com.bloxbean.cardano.client.util.HexUtil;
import org.cardanofoundation.tokenmetadata.registry.api.model.v2.ValidationScope;
import org.cardanofoundation.tokenmetadata.registry.api.model.validate.Cip113ValidateRequest;
import org.cardanofoundation.tokenmetadata.registry.api.model.validate.Cip26ValidateRequest;
import org.cardanofoundation.tokenmetadata.registry.api.model.validate.Cip68Fields;
import org.cardanofoundation.tokenmetadata.registry.api.model.validate.Cip68ValidateRequest;
import org.cardanofoundation.tokenmetadata.registry.api.model.validate.ServedAs;
import org.cardanofoundation.tokenmetadata.registry.api.model.validate.ValidateResponse;
import org.cardanofoundation.tokenmetadata.registry.api.service.cip113.Cip113RegistryNodeParser;
import org.cardanofoundation.tokenmetadata.registry.model.Item;
import org.cardanofoundation.tokenmetadata.registry.model.Mapping;
import org.cardanofoundation.tokenmetadata.registry.repository.MetadataReferenceNftRepository;
import org.cardanofoundation.tokenmetadata.registry.util.TokenMetadataValidator;
import org.cardanofoundation.tokenmetadata.registry.validation.FieldCategory;
import org.cardanofoundation.tokenmetadata.registry.validation.ValidationError;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * The validate endpoint answers with what indexing would do (ADR-023): the real CIP-26 validator, CIP-68 parser and
 * per-label rules, and CIP-113 node parser, nothing mocked but the repository it never reads.
 */
class TokenValidationServiceTest {

    private static final String POLICY = "aa".repeat(28);

    private final Cip68FTDatumParser parser = new Cip68FTDatumParser();
    private final TokenValidationService service = new TokenValidationService(new TokenMetadataValidator(), parser,
            new Cip68FungibleTokenService(mock(MetadataReferenceNftRepository.class), parser), new Cip113RegistryNodeParser());

    private static Cip68Fields fields(String name, String description, String logo, String image, Long version) {
        return new Cip68Fields(name, description, "TKN", "https://example.com", logo, image, null, 6L, version);
    }

    private static ServedAs label(ValidateResponse response, int label) {
        return response.served().stream().filter(s -> s.label() == label).findFirst().orElseThrow();
    }

    @Nested
    class Cip68 {

        @Test
        void aFungibleTokenThatFollowsCip68IsStoredAndServedUnderEveryScope() {
            ValidateResponse response = service.validateCip68(new Cip68ValidateRequest(null,
                    fields("My Token", "Useful", "ipfs://bafy", null, 1L), null));

            assertThat(response.stored()).isTrue();
            assertThat(response.validationErrors()).isEmpty();
            ServedAs ft = label(response, 333);
            assertThat(ft.metadata().name().value()).isEqualTo("My Token");
            assertThat(ft.metadata().decimals().value()).isEqualTo(6L);
            assertThat(ft.metadata().valid()).isTrue();
            assertThat(ft.scopes()).containsExactly(ValidationScope.values());
        }

        @Test
        void returnsTheDatumItBuiltAndTheParserReadsItBack() {
            ValidateResponse response = service.validateCip68(new Cip68ValidateRequest(null,
                    fields("My Token", "Useful", null, null, 2L), null));

            assertThat(response.datum()).isNotBlank();
            assertThat(parser.parse(response.datum())).hasValueSatisfying(parsed -> {
                assertThat(parsed.name()).isEqualTo("My Token");
                assertThat(parsed.version()).isEqualTo(2L);
            });
        }

        @Test
        void reportsTheErrorsOfEachLabel() {
            // no description: a 333 token needs one, a 444 token does not (it needs an image)
            ValidateResponse response = service.validateCip68(new Cip68ValidateRequest(null,
                    fields("Rich", null, null, "ipfs://bafyimage", 1L), List.of(333, 444)));

            assertThat(response.stored()).isTrue();
            ServedAs ft = label(response, 333);
            assertThat(ft.validationErrors()).extracting(ValidationError::fieldName, ValidationError::fieldCategory)
                    .containsExactly(org.assertj.core.groups.Tuple.tuple("description", FieldCategory.MANDATORY));
            assertThat(ft.scopes()).containsExactly(ValidationScope.ALL);

            ServedAs rft = label(response, 444);
            assertThat(rft.validationErrors()).isEmpty();
            assertThat(rft.metadata().logo().value()).as("a 444 token's image is served as its logo").isEqualTo("ipfs://bafyimage");
        }

        @Test
        void aLogoThatIsNotAnAllowedUriIsAnOptionalError() {
            ValidateResponse response = service.validateCip68(new Cip68ValidateRequest(null,
                    fields("My Token", "Useful", "QmBareCid", null, 1L), null));

            ServedAs ft = label(response, 333);
            assertThat(ft.validationErrors()).singleElement().satisfies(error -> {
                assertThat(error.code()).isEqualTo("invalid_uri_scheme");
                assertThat(error.fieldCategory()).isEqualTo(FieldCategory.OPTIONAL);
            });
            assertThat(ft.scopes()).containsExactly(ValidationScope.ALL, ValidationScope.MANDATORY_ONLY);
            assertThat(ft.metadata().logo().value()).as("kept as written").isEqualTo("QmBareCid");
        }

        @Test
        void aVersionCip68DoesNotDefineIsAMandatoryError() {
            ValidateResponse response = service.validateCip68(new Cip68ValidateRequest(null,
                    fields("My Token", "Useful", null, null, 0L), null));

            assertThat(response.validationErrors()).extracting(ValidationError::code).contains("undefined_version");
        }

        @Test
        void aDatumThatCannotBeDecodedWouldBeDropped() {
            ValidateResponse response = service.validateCip68(new Cip68ValidateRequest("d8799fff00ff", null, null));

            assertThat(response.stored()).isFalse();
            assertThat(response.dropped()).isNotNull();
            assertThat(response.served()).isEmpty();
        }

        @Test
        void aDatumThatIsNotCip68IsIgnored() throws Exception {
            String integer = HexUtil.encodeHexString(CborSerializationUtil.serialize(BigIntPlutusData.of(42).serialize()));

            ValidateResponse response = service.validateCip68(new Cip68ValidateRequest(integer, null, null));

            assertThat(response.stored()).isFalse();
            assertThat(response.dropped().code()).isEqualTo("not_cip68");
        }

        @Test
        void asksForEitherADatumOrFields() {
            assertThatThrownBy(() -> service.validateCip68(new Cip68ValidateRequest(null, null, null)))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> service.validateCip68(new Cip68ValidateRequest("d87980",
                    fields("A", "B", null, null, 1L), null))).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void acceptsOnlyTheLabelsTheRegistryServes() {
            assertThatThrownBy(() -> service.validateCip68(new Cip68ValidateRequest(null,
                    fields("A", "B", null, null, 1L), List.of(222)))).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("222");
        }
    }

    @Nested
    class Cip26 {

        private final String subject = POLICY + "4d544b";

        private Mapping mapping(String subject, String name) {
            return new Mapping(subject, null, item(name), item("MTK"), item("6"), null, null, item("A useful token"));
        }

        private Item item(String value) {
            return new Item(0, value, List.of());
        }

        @Test
        void anEntryThatFollowsCip26IsStoredWithoutErrors() {
            ValidateResponse response = service.validateCip26(new Cip26ValidateRequest(mapping(subject, "My Token"), subject + ".json"));

            assertThat(response.stored()).isTrue();
            assertThat(response.validationErrors()).isEmpty();
            ServedAs served = response.served().getFirst();
            assertThat(served.label()).isNull();
            assertThat(served.metadata().name().value()).isEqualTo("My Token");
            assertThat(served.metadata().name().source()).isEqualTo("CIP_26");
            assertThat(served.scopes()).containsExactly(ValidationScope.values());
        }

        @Test
        void aNameLongerThanCip26AllowsIsAMandatoryError() {
            ValidateResponse response = service.validateCip26(new Cip26ValidateRequest(mapping(subject, "N".repeat(60)), null));

            assertThat(response.stored()).isTrue();
            assertThat(response.validationErrors()).anySatisfy(error -> {
                assertThat(error.fieldName()).isEqualTo("name");
                assertThat(error.fieldCategory()).isEqualTo(FieldCategory.MANDATORY);
            });
            assertThat(response.served().getFirst().scopes()).containsExactly(ValidationScope.ALL);
        }

        @Test
        void aFileNamedAfterAnotherSubjectWouldBeDropped() {
            ValidateResponse response = service.validateCip26(new Cip26ValidateRequest(mapping(subject, "My Token"),
                    POLICY + "6f74686572.json"));

            assertThat(response.stored()).isFalse();
            assertThat(response.dropped().code()).isEqualTo("filename_subject_mismatch");
        }

        @Test
        void aSubjectThatCannotBeAKeyWouldBeDropped() {
            ValidateResponse response = service.validateCip26(new Cip26ValidateRequest(mapping("aaaa", "My Token"), null));

            assertThat(response.stored()).isFalse();
            assertThat(response.dropped().code()).isEqualTo("unusable_subject");
        }
    }

    @Nested
    class Cip113 {

        private static final String MINTING = "999513b0fcc01d635f8535d49f38acc33d4d6b62ee8732ca6e126101";
        private static final String TRANSFER = "aaa513b0fcc01d635f8535d49f38acc33d4d6b62ee8732ca6e126102";

        private String node() throws Exception {
            PlutusData datum = ConstrPlutusData.of(0,
                    BytesPlutusData.of(HexUtil.decodeHexString(POLICY)),
                    BytesPlutusData.of(HexUtil.decodeHexString("ff".repeat(28))),
                    ConstrPlutusData.of(0, BytesPlutusData.of(HexUtil.decodeHexString(MINTING))),
                    ConstrPlutusData.of(0, BytesPlutusData.of(HexUtil.decodeHexString(TRANSFER))),
                    ConstrPlutusData.of(0, BytesPlutusData.of(HexUtil.decodeHexString(TRANSFER))),
                    BytesPlutusData.of(new byte[0]),
                    BytesPlutusData.of(new byte[0]));
            return HexUtil.encodeHexString(CborSerializationUtil.serialize(datum.serialize()));
        }

        @Test
        void aRegistryNodeIsReadIntoTheExtensionV2Returns() throws Exception {
            ValidateResponse response = service.validateCip113(new Cip113ValidateRequest(node()));

            assertThat(response.stored()).isTrue();
            assertThat(response.cip113().mintingLogicScript()).isEqualTo(MINTING);
            assertThat(response.cip113().transferLogicScript()).isEqualTo(TRANSFER);
            assertThat(response.cip113().unfrackingLogicScript()).as("empty means none").isNull();
            assertThat(response.notes()).anyMatch(note -> note.contains("CIP-26 or CIP-68"));
        }

        @Test
        void aNodeThatCannotBeReadWouldBeDropped() {
            ValidateResponse response = service.validateCip113(new Cip113ValidateRequest("d87980"));

            assertThat(response.stored()).isFalse();
            assertThat(response.dropped().code()).isEqualTo("registry_node_unparseable");
        }
    }
}
