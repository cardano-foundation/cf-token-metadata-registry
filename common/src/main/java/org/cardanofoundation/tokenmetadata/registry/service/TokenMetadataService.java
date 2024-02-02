package org.cardanofoundation.tokenmetadata.registry.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.model.Item;
import org.cardanofoundation.tokenmetadata.registry.model.Mapping;
import org.cardanofoundation.tokenmetadata.registry.persistence.TokenMetadataDao;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
@Slf4j
@AllArgsConstructor
public class TokenMetadataService {

    private static final String SOURCE = "mainnet";

    private final TokenMetadataDao tokenMetadataDao;

    public void insertMapping(Mapping mapping, LocalDateTime updatedAt, String updateBy) {
        tokenMetadataDao.insertTokenMetadata(mapping.subject(),
                SOURCE,
                Optional.ofNullable(mapping.policy()),
                Optional.ofNullable(mapping.name()).map(Item::value),
                Optional.ofNullable(mapping.ticker()).map(Item::value),
                Optional.ofNullable(mapping.url()).map(Item::value),
                Optional.ofNullable(mapping.description()).map(Item::value),
                Optional.ofNullable(mapping.decimals()).map(Item::value).map(Integer::valueOf),
                Timestamp.valueOf(updatedAt),
                updateBy,
                mapping
        );
    }

    public void insertLogo(Mapping mapping) {
        tokenMetadataDao.insertTokenLogo(mapping.subject(),
                SOURCE,
                Optional.ofNullable(mapping.logo()).map(Item::value));
    }

}
