package org.cardanofoundation.tokenmetadata.registry.service;

import org.cardanofoundation.tokenmetadata.registry.persistence.TokenMetadataDao;
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
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

@SpringBootTest(classes = {TokenMappingService.class, TokenMetadataService.class})
class TokenMetadataServiceTest {
    @Autowired
    private TokenMappingService tokenMappingService;
    @Autowired
    private TokenMetadataService tokenMetadataService;
    @MockBean
    private TokenMetadataDao tokenMetadataDao;

    @Test
    public void insertMappingTest() throws IOException {

        var mappingFile = ResourceUtils.getFile("classpath:mappings/ff7cad970d3a755a1ff0335ccb3f3c1cabf31aacf3f23dd13db61b0630313030.json");
        var mappingsOpt = tokenMappingService.parseMappings(mappingFile);

        Assertions.assertTrue(mappingsOpt.isPresent());

        mappingsOpt.ifPresent(mappings -> {
            var now = LocalDateTime.now();
            var testUser = "test-user";
            tokenMetadataService.insertMapping(mappings, now, testUser);

            Mockito.verify(tokenMetadataDao, Mockito.times(1)).insertTokenMetadata("ff7cad970d3a755a1ff0335ccb3f3c1cabf31aacf3f23dd13db61b0630313030",
                    "mainnet",
                    Optional.of("820182018282051a02f893828200581cdc1ac66efbf0f27457cd646d80fbeee08eafcacce42fb631ca1a0254"),
                    Optional.of("Spirit Of The Bone Forest"),
                    Optional.of("0100"),
                    Optional.of("https://adage.app/nft-giveaway"),
                    Optional.of("Part of the ADAGE NFT giveaway and Veritree donation"),
                    Optional.empty(),
                    Timestamp.valueOf(now),
                    testUser,
                    mappings);

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
            tokenMetadataService.insertLogo(mappings);

            Mockito.verify(tokenMetadataDao, Mockito.times(1)).insertTokenLogo("ff7cad970d3a755a1ff0335ccb3f3c1cabf31aacf3f23dd13db61b0630313030",
                    "mainnet",
                    Optional.of(logo));

        });


    }


}