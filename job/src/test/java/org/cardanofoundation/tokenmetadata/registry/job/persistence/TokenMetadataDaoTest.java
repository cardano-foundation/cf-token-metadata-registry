package org.cardanofoundation.tokenmetadata.registry.job.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.job.config.DatasourceConfig;
import org.cardanofoundation.tokenmetadata.registry.job.model.Item;
import org.cardanofoundation.tokenmetadata.registry.job.model.Mapping;
import org.cardanofoundation.tokenmetadata.registry.job.service.GitService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.IOException;
import java.sql.Date;
import java.util.List;
import java.util.Optional;

@SpringBootTest(classes = {GitService.class, TokenMetadataDao.class, DatasourceConfig.class})
@ActiveProfiles("test")
@Slf4j
class TokenMetadataDaoTest {

    @Autowired
    private TokenMetadataDao tokenMetadataDao;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testInsert() {

        List.of("00000002df633853f6a47465c9496721d2d5b1291b8398016c0e87ae6e7574636f696e.json",
                        "0002b854b7b086fc6c2f9bd37347214f637f59f6bbd73f6f839036e043484144.json")
                .forEach(mappingFileName -> {
                    try {
                        var mapping = objectMapper.readValue(new File("/tmp/cardano-token-registry/mappings/" + mappingFileName), Mapping.class);
                        tokenMetadataDao.insertTokenMetadata(mapping.subject(),
                                "mainnet",
                                Optional.ofNullable(mapping.policy()),
                                Optional.ofNullable(mapping.name()).map(Item::value),
                                Optional.ofNullable(mapping.ticker()).map(Item::value),
                                Optional.ofNullable(mapping.url()).map(Item::value),
                                Optional.ofNullable(mapping.description()).map(Item::value),
                                Optional.ofNullable(mapping.decimals()).map(Item::value).map(Integer::valueOf),
                                new Date(new java.util.Date().getTime()),
                                "test",
                                mapping
                        );
                    } catch (IOException e) {
                        log.info(mappingFileName, e);
                    }

                });

    }

}