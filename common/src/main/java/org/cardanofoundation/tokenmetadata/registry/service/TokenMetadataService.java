package org.cardanofoundation.tokenmetadata.registry.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.entity.TokenMetadata;
import org.cardanofoundation.tokenmetadata.registry.model.Item;
import org.cardanofoundation.tokenmetadata.registry.model.Mapping;
import org.cardanofoundation.tokenmetadata.registry.persistence.TokenMetadataDao;
import org.cardanofoundation.tokenmetadata.registry.repository.TokenMetadataRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Function;

@Component
@Slf4j
@AllArgsConstructor
public class TokenMetadataService {

    private final TokenMetadataDao tokenMetadataDao;

    private final TokenMetadataRepository tokenMetadataRepository;

    public void insertMapping(Mapping mapping, LocalDateTime updatedAt, String updateBy) {

        var tokenMetadata = new TokenMetadata();
        tokenMetadata.setSubject(mapping.subject());
        tokenMetadata.setPolicy(mapping.policy());
        tokenMetadata.setName(getValue(mapping.name()));
        tokenMetadata.setTicker(getValue(mapping.ticker()));
        tokenMetadata.setUrl(getValue(mapping.url()));
        tokenMetadata.setDescription(getValue(mapping.description()));
        tokenMetadata.setDecimals(getValue(mapping.decimals(), Long::valueOf));
        tokenMetadata.setUpdated(updatedAt);
        tokenMetadata.setUpdatedBy(updateBy);
        tokenMetadata.setProperties(mapping);

        tokenMetadataRepository.save(tokenMetadata);

    }

    private static String getValue(Item item) {
        return getValue(item, Function.identity());
    }

    private static <T> T getValue(Item item, Function<String, T> f) {
        return Optional.ofNullable(item).map(Item::value).map(f).orElse(null);
    }

    public void insertLogo(Mapping mapping) {
        tokenMetadataDao.insertTokenLogo(mapping.subject(), Optional.ofNullable(mapping.logo()).map(Item::value));
    }

}
