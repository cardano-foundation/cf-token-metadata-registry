package org.cardanofoundation.tokenmetadata.registry.util;

import org.cardanofoundation.tokenmetadata.registry.config.JsonConfiguration;
import org.cardanofoundation.tokenmetadata.registry.entity.TokenLogo;
import org.cardanofoundation.tokenmetadata.registry.entity.TokenMetadata;
import org.cardanofoundation.tokenmetadata.registry.service.TokenMappingService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.ResourceUtils;

import org.cardanofoundation.tokenmetadata.registry.model.Mapping;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

@SpringBootTest(classes = {TokenMappingService.class, JsonConfiguration.class})
class MappingsUtilTest {

    @Autowired
    private TokenMappingService tokenMappingService;

    @Test
    void testTokenMetadata() throws FileNotFoundException {
        File mappingFile = ResourceUtils.getFile("classpath:mappings/4ffaa4ef3217df37c4995bb96066af4cb68dfcc66b9f2a10e0c333b95779726d73746f6e65.json");
        Optional<Mapping> mappingsOpt = tokenMappingService.parseMappings(mappingFile);

        Assertions.assertTrue(mappingsOpt.isPresent());

        String updatedBy = "test-user";
        LocalDateTime now = LocalDateTime.now();

        TokenMetadata expectedTokenMetadata = new TokenMetadata();
        expectedTokenMetadata.setSubject("4ffaa4ef3217df37c4995bb96066af4cb68dfcc66b9f2a10e0c333b95779726d73746f6e65");
        expectedTokenMetadata.setPolicy("82008200581ce943575bd64841810b1110f1801572fd8b7ec15a57ca49e15e2690c7");
        expectedTokenMetadata.setUrl("https://tavernsquad.io");
        expectedTokenMetadata.setName("Wyrmstone");
        expectedTokenMetadata.setTicker("WYRM");
        expectedTokenMetadata.setDecimals(6L);
        expectedTokenMetadata.setDescription("The official utility token of Tavern Squad");
        expectedTokenMetadata.setProperties(mappingsOpt.get());
        expectedTokenMetadata.setUpdated(now);
        expectedTokenMetadata.setUpdatedBy(updatedBy);

        mappingsOpt.ifPresent(mappings -> {
            TokenMetadata actual = MappingsUtil.toTokenMetadata(mappings, updatedBy, now);
            org.assertj.core.api.Assertions.assertThat(actual).usingRecursiveComparison().isEqualTo(expectedTokenMetadata);
        });


    }

    @Test
    void storesASubjectInLowerCaseWhateverTheCaseOfTheRegistryFile() {
        // mainnet: 39 registry files have an upper-case subject; a subject is base16, its case carries no meaning
        Mapping mapping = new Mapping("F106CF30E10297C20D96EC76770B4BAB585DDB8A92CBE0E9DC3931A34269746D616E6961",
                null, null, null, null, null, null, null);

        org.assertj.core.api.Assertions.assertThat(MappingsUtil.toTokenMetadata(mapping, "test-user", LocalDateTime.now()).getSubject())
                .isEqualTo("f106cf30e10297c20d96ec76770b4bab585ddb8a92cbe0e9dc3931a34269746d616e6961");
        org.assertj.core.api.Assertions.assertThat(MappingsUtil.toTokenLogo(mapping).getSubject())
                .isEqualTo("f106cf30e10297c20d96ec76770b4bab585ddb8a92cbe0e9dc3931a34269746d616e6961");
        org.assertj.core.api.Assertions.assertThat(MappingsUtil.normalizeSubject(null)).isNull();
    }

    @Test
    void testTokenLogo() throws IOException {

        File mappingFile = ResourceUtils.getFile("classpath:mappings/ff7cad970d3a755a1ff0335ccb3f3c1cabf31aacf3f23dd13db61b0630313030.json");
        File logoFile = ResourceUtils.getFile("classpath:mappings/ff7cad970d3a755a1ff0335ccb3f3c1cabf31aacf3f23dd13db61b0630313030-logo.txt");
        String logo;
        try (BufferedReader reader = new BufferedReader(new FileReader(logoFile))) {
            logo = reader.readLine();
        }

        Optional<Mapping> mappingsOpt = tokenMappingService.parseMappings(mappingFile);

        Assertions.assertTrue(mappingsOpt.isPresent());

        TokenLogo expected = new TokenLogo();
        expected.setSubject("ff7cad970d3a755a1ff0335ccb3f3c1cabf31aacf3f23dd13db61b0630313030");
        expected.setLogo(logo);

        mappingsOpt.ifPresent(mappings -> {
            TokenLogo actual = MappingsUtil.toTokenLogo(mappingsOpt.get());
            org.assertj.core.api.Assertions.assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
        });


    }


}