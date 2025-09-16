package org.cardanofoundation.tokenmetadata.registry.service;

import org.cardanofoundation.tokenmetadata.registry.config.JsonConfiguration;
import org.cardanofoundation.tokenmetadata.registry.entity.TokenLogo;
import org.cardanofoundation.tokenmetadata.registry.entity.TokenMetadata;
import org.cardanofoundation.tokenmetadata.registry.repository.TokenLogoRepository;
import org.cardanofoundation.tokenmetadata.registry.repository.TokenMetadataRepository;
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

@SpringBootTest(classes = {TokenMappingService.class, TokenMetadataService.class, JsonConfiguration.class})
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

            tokenMetadataService.insertMapping(mappings, now, testUser);

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

            tokenMetadataService.insertLogo(mappings);

            Mockito.verify(tokenLogoRepository, Mockito.times(1)).save(tokenLogo);

        });


    }


}