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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;

@SpringBootTest(classes = {TokenMappingService.class, JsonConfiguration.class})
class MappingsUtilTest {

    @Autowired
    private TokenMappingService tokenMappingService;

    @Test
    public void testTokenMetadata() throws FileNotFoundException {
        var mappingFile = ResourceUtils.getFile("classpath:mappings/4ffaa4ef3217df37c4995bb96066af4cb68dfcc66b9f2a10e0c333b95779726d73746f6e65.json");
        var mappingsOpt = tokenMappingService.parseMappings(mappingFile);

        Assertions.assertTrue(mappingsOpt.isPresent());

        var updatedBy = "test-user";
        var now = LocalDateTime.now();

        var expectedTokenMetadata = new TokenMetadata();
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
            var actual = MappingsUtil.toTokenMetadata(mappings, updatedBy, now);
            org.assertj.core.api.Assertions.assertThat(actual).usingRecursiveComparison().isEqualTo(expectedTokenMetadata);
        });


    }

    @Test
    public void testTokenLogo() throws IOException {

        var mappingFile = ResourceUtils.getFile("classpath:mappings/ff7cad970d3a755a1ff0335ccb3f3c1cabf31aacf3f23dd13db61b0630313030.json");
        var logoFile = ResourceUtils.getFile("classpath:mappings/ff7cad970d3a755a1ff0335ccb3f3c1cabf31aacf3f23dd13db61b0630313030-logo.txt");
        var logo = new BufferedReader(new FileReader(logoFile)).readLine();

        var mappingsOpt = tokenMappingService.parseMappings(mappingFile);

        Assertions.assertTrue(mappingsOpt.isPresent());

        var expected = new TokenLogo();
        expected.setSubject("ff7cad970d3a755a1ff0335ccb3f3c1cabf31aacf3f23dd13db61b0630313030");
        expected.setLogo(logo);

        mappingsOpt.ifPresent(mappings -> {
            var actual = MappingsUtil.toTokenLogo(mappingsOpt.get());
            org.assertj.core.api.Assertions.assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
        });


    }


}