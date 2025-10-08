package org.cardanofoundation.tokenmetadata.registry.service;

import org.cardanofoundation.tokenmetadata.registry.config.JsonConfiguration;
import org.cardanofoundation.tokenmetadata.registry.entity.TokenLogo;
import org.cardanofoundation.tokenmetadata.registry.entity.TokenMetadata;
import org.cardanofoundation.tokenmetadata.registry.repository.TokenLogoRepository;
import org.cardanofoundation.tokenmetadata.registry.repository.TokenMetadataRepository;
import org.cardanofoundation.tokenmetadata.registry.util.TokenMetadataValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.util.ResourceUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;

@SpringBootTest(classes = {TokenMappingService.class, TokenMetadataService.class, TokenMetadataValidator.class, JsonConfiguration.class})
class TokenMetadataServiceTest {

    @Autowired
    private TokenMappingService tokenMappingService;
    @Autowired
    private TokenMetadataService tokenMetadataService;

    @MockBean
    private TokenMetadataRepository tokenMetadataRepository;
    @MockBean
    private TokenLogoRepository tokenLogoRepository;

    @Test
    public void insertMappingTest() throws IOException {

        var mappingFile = ResourceUtils.getFile("classpath:mappings/ff7cad970d3a755a1ff0335ccb3f3c1cabf31aacf3f23dd13db61b0630313030.json");
        var mappingsOpt = tokenMappingService.parseMappings(mappingFile);

        Assertions.assertTrue(mappingsOpt.isPresent());

        mappingsOpt.ifPresent(mappings -> {
            var now = LocalDateTime.now();
            var testUser = "test-user";

            var tokenMetadata = new TokenMetadata();
            tokenMetadata.setSubject("ff7cad970d3a755a1ff0335ccb3f3c1cabf31aacf3f23dd13db61b0630313030");
            tokenMetadata.setPolicy("820182018282051a02f893828200581cdc1ac66efbf0f27457cd646d80fbeee08eafcacce42fb631ca1a0254");
            tokenMetadata.setName("Spirit Of The Bone Forest");
            tokenMetadata.setTicker("0100");
            tokenMetadata.setUrl("https://adage.app/nft-giveaway");
            tokenMetadata.setDescription("Part of the ADAGE NFT giveaway and Veritree donation");
            tokenMetadata.setUpdated(now);
            tokenMetadata.setUpdatedBy(testUser);
            tokenMetadata.setProperties(mappings);

            // Verify that insertMapping returns true for valid token
            boolean result = tokenMetadataService.insertMapping(mappings, now, testUser);
            Assertions.assertTrue(result, "insertMapping should return true for valid token");

            // Verify repository save was called
            Mockito.verify(tokenMetadataRepository, Mockito.times(1)).save(tokenMetadata);

        });


    }

    @Test
    public void insertLogoTest() throws IOException {

        var mappingFile = ResourceUtils.getFile("classpath:mappings/ff7cad970d3a755a1ff0335ccb3f3c1cabf31aacf3f23dd13db61b0630313030.json");
        var logoFile = ResourceUtils.getFile("classpath:mappings/ff7cad970d3a755a1ff0335ccb3f3c1cabf31aacf3f23dd13db61b0630313030-logo.txt");
        var mappingsOpt = tokenMappingService.parseMappings(mappingFile);

        Assertions.assertTrue(mappingsOpt.isPresent());

        var logo = new BufferedReader(new FileReader(logoFile)).readLine();

        mappingsOpt.ifPresent(mappings -> {

            var tokenLogo = new TokenLogo();
            tokenLogo.setSubject("ff7cad970d3a755a1ff0335ccb3f3c1cabf31aacf3f23dd13db61b0630313030");
            tokenLogo.setLogo(logo);

            // Verify that insertLogo returns true for valid logo
            boolean result = tokenMetadataService.insertLogo(mappings);
            Assertions.assertTrue(result, "insertLogo should return true for valid logo");

            // Verify repository save was called
            Mockito.verify(tokenLogoRepository, Mockito.times(1)).save(tokenLogo);

        });


    }

    @Test
    public void insertMappingTest_ShouldRejectTokenWithNameExceedingMaxLength() {
        var now = LocalDateTime.now();
        var testUser = "test-user";

        // Create a mapping with a name that exceeds 255 characters
        var longName = "a".repeat(256);
        var mapping = new org.cardanofoundation.tokenmetadata.registry.model.Mapping(
                "testSubject",
                new org.cardanofoundation.tokenmetadata.registry.model.Item(null, "https://example.com", null), // url
                new org.cardanofoundation.tokenmetadata.registry.model.Item(null, longName, null), // name
                new org.cardanofoundation.tokenmetadata.registry.model.Item(null, "TICK", null), // ticker
                new org.cardanofoundation.tokenmetadata.registry.model.Item(null, "6", null), // decimals
                new org.cardanofoundation.tokenmetadata.registry.model.Item(null, "data:image/png;base64,abc", null), // logo
                "testPolicy",
                new org.cardanofoundation.tokenmetadata.registry.model.Item(null, "description", null) // description
        );

        // Verify that insertMapping returns false for invalid token
        boolean result = tokenMetadataService.insertMapping(mapping, now, testUser);
        Assertions.assertFalse(result, "insertMapping should return false when name exceeds 255 characters");

        // Verify repository save was NOT called
        Mockito.verify(tokenMetadataRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    public void insertMappingTest_ShouldRejectTokenWithTickerExceedingMaxLength() {
        var now = LocalDateTime.now();
        var testUser = "test-user";

        // Create a mapping with a ticker that exceeds 32 characters
        var longTicker = "a".repeat(33);
        var mapping = new org.cardanofoundation.tokenmetadata.registry.model.Mapping(
                "testSubject",
                new org.cardanofoundation.tokenmetadata.registry.model.Item(null, "https://example.com", null), // url
                new org.cardanofoundation.tokenmetadata.registry.model.Item(null, "TestToken", null), // name
                new org.cardanofoundation.tokenmetadata.registry.model.Item(null, longTicker, null), // ticker
                new org.cardanofoundation.tokenmetadata.registry.model.Item(null, "6", null), // decimals
                new org.cardanofoundation.tokenmetadata.registry.model.Item(null, "data:image/png;base64,abc", null), // logo
                "testPolicy",
                new org.cardanofoundation.tokenmetadata.registry.model.Item(null, "description", null) // description
        );

        // Verify that insertMapping returns false for invalid token
        boolean result = tokenMetadataService.insertMapping(mapping, now, testUser);
        Assertions.assertFalse(result, "insertMapping should return false when ticker exceeds 32 characters");

        // Verify repository save was NOT called
        Mockito.verify(tokenMetadataRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    public void insertMappingTest_ShouldRejectTokenWithUrlExceedingMaxLength() {
        var now = LocalDateTime.now();
        var testUser = "test-user";

        // Create a mapping with a URL that exceeds 255 characters
        var longUrl = "https://example.com/" + "a".repeat(256);
        var mapping = new org.cardanofoundation.tokenmetadata.registry.model.Mapping(
                "testSubject",
                new org.cardanofoundation.tokenmetadata.registry.model.Item(null, longUrl, null), // url
                new org.cardanofoundation.tokenmetadata.registry.model.Item(null, "TestToken", null), // name
                new org.cardanofoundation.tokenmetadata.registry.model.Item(null, "TICK", null), // ticker
                new org.cardanofoundation.tokenmetadata.registry.model.Item(null, "6", null), // decimals
                new org.cardanofoundation.tokenmetadata.registry.model.Item(null, "data:image/png;base64,abc", null), // logo
                "testPolicy",
                new org.cardanofoundation.tokenmetadata.registry.model.Item(null, "description", null) // description
        );

        // Verify that insertMapping returns false for invalid token
        boolean result = tokenMetadataService.insertMapping(mapping, now, testUser);
        Assertions.assertFalse(result, "insertMapping should return false when URL exceeds 255 characters");

        // Verify repository save was NOT called
        Mockito.verify(tokenMetadataRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    public void insertLogoTest_ShouldRejectLogoWithSubjectExceedingMaxLength() {
        // Create a mapping with a subject that exceeds 255 characters
        var longSubject = "a".repeat(256);
        var mapping = new org.cardanofoundation.tokenmetadata.registry.model.Mapping(
                longSubject, // subject exceeds 255
                new org.cardanofoundation.tokenmetadata.registry.model.Item(null, "https://example.com", null), // url
                new org.cardanofoundation.tokenmetadata.registry.model.Item(null, "TestToken", null), // name
                new org.cardanofoundation.tokenmetadata.registry.model.Item(null, "TICK", null), // ticker
                new org.cardanofoundation.tokenmetadata.registry.model.Item(null, "6", null), // decimals
                new org.cardanofoundation.tokenmetadata.registry.model.Item(null, "data:image/png;base64,abc", null), // logo
                "testPolicy",
                new org.cardanofoundation.tokenmetadata.registry.model.Item(null, "description", null) // description
        );

        // Verify that insertLogo returns false for invalid subject
        boolean result = tokenMetadataService.insertLogo(mapping);
        Assertions.assertFalse(result, "insertLogo should return false when subject exceeds 255 characters");

        // Verify repository save was NOT called
        Mockito.verify(tokenLogoRepository, Mockito.never()).save(Mockito.any());
    }


}