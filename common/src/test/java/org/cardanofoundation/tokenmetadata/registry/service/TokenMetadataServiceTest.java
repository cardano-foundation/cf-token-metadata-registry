package org.cardanofoundation.tokenmetadata.registry.service;

import org.cardanofoundation.tokenmetadata.registry.config.JsonConfiguration;
import org.cardanofoundation.tokenmetadata.registry.entity.TokenLogo;
import org.cardanofoundation.tokenmetadata.registry.entity.TokenMetadata;
import org.cardanofoundation.tokenmetadata.registry.invalidtoken.InvalidTokenReason;
import org.cardanofoundation.tokenmetadata.registry.invalidtoken.InvalidTokenRecorder;
import org.cardanofoundation.tokenmetadata.registry.invalidtoken.InvalidTokenReport;
import org.cardanofoundation.tokenmetadata.registry.model.Item;
import org.cardanofoundation.tokenmetadata.registry.model.Mapping;
import org.cardanofoundation.tokenmetadata.registry.repository.TokenLogoRepository;
import org.cardanofoundation.tokenmetadata.registry.repository.TokenMetadataRepository;
import org.cardanofoundation.tokenmetadata.registry.util.TokenMetadataValidator;
import org.cardanofoundation.tokenmetadata.registry.validation.FieldCategory;
import org.cardanofoundation.tokenmetadata.registry.validation.ValidationError;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * A CIP-26 entry is stored as written, with its validation errors, whatever it breaks; only an entry whose subject cannot
 * be a key is dropped, and recorded in {@code invalid_token} under its registry file (ADR-019, ADR-020).
 */
@SpringBootTest(classes = {TokenMappingService.class, TokenMetadataService.class, TokenMetadataValidator.class, JsonConfiguration.class})
class TokenMetadataServiceTest {

    private static final String SUBJECT = "ff7cad970d3a755a1ff0335ccb3f3c1cabf31aacf3f23dd13db61b0630313030";
    private static final String ORIGIN = SUBJECT + ".json";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 21, 12, 0);

    @Autowired
    private TokenMappingService tokenMappingService;
    @Autowired
    private TokenMetadataService tokenMetadataService;

    @MockitoBean
    private TokenMetadataRepository tokenMetadataRepository;
    @MockitoBean
    private TokenLogoRepository tokenLogoRepository;
    @MockitoBean
    private InvalidTokenRecorder invalidTokenRecorder;

    @Nested
    class EntriesThatFollowCip26 {

        @Test
        void storesTheEntryWithNoErrorsAndItsLogo() throws IOException {
            Mapping mapping = registryFile();

            assertThat(tokenMetadataService.insertMapping(mapping, NOW, "test-user", ORIGIN)).isTrue();

            TokenMetadata stored = savedMetadata();
            assertThat(stored.getSubject()).isEqualTo(SUBJECT);
            assertThat(stored.getName()).isEqualTo("Spirit Of The Bone Forest");
            assertThat(stored.getValidationErrors()).isEmpty();
            ArgumentCaptor<TokenLogo> logo = ArgumentCaptor.forClass(TokenLogo.class);
            verify(tokenLogoRepository).save(logo.capture());
            assertThat(logo.getValue().getLogo()).isEqualTo(logoOf(mapping));
            verifyNoInteractions(invalidTokenRecorder);
        }
    }

    @Nested
    class EntriesThatBreakCip26 {

        @Test
        void storesANameOverTheLimitWithAMandatoryError() {
            assertThat(tokenMetadataService.insertMapping(mapping("A".repeat(51), "NUT", "Description", null), NOW, "u", ORIGIN)).isTrue();

            assertThat(savedMetadata().getName()).hasSize(51);
            assertThat(savedMetadata().getValidationErrors()).singleElement().satisfies(error -> {
                assertThat(error.fieldName()).isEqualTo("name");
                assertThat(error.fieldCategory()).isEqualTo(FieldCategory.MANDATORY);
                assertThat(error.code()).isEqualTo("invalid_length");
            });
        }

        @Test
        void storesATickerOverTheLimitWithAnOptionalError() {
            assertThat(tokenMetadataService.insertMapping(mapping("Nutcoin", "TOOLONGTICKER", "Description", null), NOW, "u", ORIGIN)).isTrue();

            assertThat(savedMetadata().getValidationErrors()).extracting(ValidationError::fieldName, ValidationError::fieldCategory)
                    .containsExactly(tuple("ticker", FieldCategory.OPTIONAL));
        }

        @Test
        void storesAnEntryWithoutNameOrDescriptionWithBothMissing() {
            assertThat(tokenMetadataService.insertMapping(mapping(null, "NUT", null, null), NOW, "u", ORIGIN)).isTrue();

            assertThat(savedMetadata().getValidationErrors()).extracting(ValidationError::fieldName, ValidationError::code)
                    .containsExactly(tuple("name", "missing_value"),
                            tuple("description", "missing_value"));
        }

        @Test
        void keepsALogoThatIsNotBase64WithAnError() {
            assertThat(tokenMetadataService.insertMapping(mapping("Nutcoin", "NUT", "Description", "not base64!"), NOW, "u", ORIGIN)).isTrue();

            assertThat(savedMetadata().getValidationErrors()).extracting(ValidationError::fieldName, ValidationError::code)
                    .containsExactly(tuple("logo", "invalid_base64"));
            verify(tokenLogoRepository).save(any(TokenLogo.class));
        }

        @Test
        void doesNotStoreALogoFarBeyondAnyRealSizeButStoresTheEntry() {
            String huge = "A".repeat(1_400_004);

            assertThat(tokenMetadataService.insertMapping(mapping("Nutcoin", "NUT", "Description", huge), NOW, "u", ORIGIN)).isTrue();

            verify(tokenLogoRepository, never()).save(any());
            verify(tokenLogoRepository).deleteById(SUBJECT);
        }
    }

    @Nested
    class DroppedEntries {

        @Test
        void dropsAnEntryWhoseSubjectCannotBeAKeyAndRecordsItUnderItsFile() {
            Mapping mapping = new Mapping("not-hex", null, item("Nutcoin"), null, null, null, null, item("Description"));

            assertThat(tokenMetadataService.insertMapping(mapping, NOW, "u", "not-hex.json")).isFalse();

            verify(tokenMetadataRepository, never()).save(any());
            ArgumentCaptor<InvalidTokenReport> report = ArgumentCaptor.forClass(InvalidTokenReport.class);
            verify(invalidTokenRecorder).report(report.capture());
            assertThat(report.getValue().standard()).isEqualTo(InvalidTokenReason.CIP_26);
            assertThat(report.getValue().reasonCode()).isEqualTo(InvalidTokenReason.UNUSABLE_SUBJECT);
            assertThat(report.getValue().origin()).isEqualTo("not-hex.json");
            assertThat(report.getValue().subject()).isEqualTo("not-hex");
        }

        @Test
        void returnsFalseWhenSavingFails() throws IOException {
            when(tokenMetadataRepository.save(any())).thenThrow(new RuntimeException("DB error"));

            assertThat(tokenMetadataService.insertMapping(registryFile(), NOW, "u", ORIGIN)).isFalse();
        }
    }

    @Nested
    class Deletion {

        @Test
        void deletesTheLogoBeforeTheMetadata() {
            assertThat(tokenMetadataService.deleteMapping(SUBJECT)).isTrue();

            // The logo table has a foreign key on metadata.subject, so the logo row must go first.
            InOrder inOrder = inOrder(tokenLogoRepository, tokenMetadataRepository);
            inOrder.verify(tokenLogoRepository).deleteById(SUBJECT);
            inOrder.verify(tokenMetadataRepository).deleteById(SUBJECT);
        }

        @Test
        void returnsFalseWhenDeletingFails() {
            doThrow(new RuntimeException("DB error")).when(tokenMetadataRepository).deleteById(SUBJECT);

            assertThat(tokenMetadataService.deleteMapping(SUBJECT)).isFalse();
        }
    }

    private Mapping registryFile() throws IOException {
        File mappingFile = ResourceUtils.getFile("classpath:mappings/" + ORIGIN);
        return tokenMappingService.parseMappings(mappingFile).orElseThrow();
    }

    private static String logoOf(Mapping mapping) {
        return mapping.logo().value();
    }

    private TokenMetadata savedMetadata() {
        ArgumentCaptor<TokenMetadata> saved = ArgumentCaptor.forClass(TokenMetadata.class);
        verify(tokenMetadataRepository).save(saved.capture());
        return saved.getValue();
    }

    private static Mapping mapping(String name, String ticker, String description, String logo) {
        return new Mapping(SUBJECT, null, item(name), item(ticker), null, item(logo), null, item(description));
    }

    private static Item item(String value) {
        return value == null ? null : new Item(0, value, List.of());
    }
}
