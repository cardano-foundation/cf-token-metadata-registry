package org.cardanofoundation.tokenmetadata.registry.util;

import org.cardanofoundation.tokenmetadata.registry.entity.TokenLogo;
import org.cardanofoundation.tokenmetadata.registry.entity.TokenMetadata;
import org.cardanofoundation.tokenmetadata.registry.model.Item;
import org.cardanofoundation.tokenmetadata.registry.model.Mapping;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Function;

public class MappingsUtil {

    public static TokenMetadata toTokenMetadata(Mapping mapping, String updateBy, LocalDateTime updatedAt) {
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
        return tokenMetadata;
    }

    public static TokenLogo toTokenLogo(Mapping mapping) {
        var tokenLogo = new TokenLogo();
        tokenLogo.setSubject(mapping.subject());
        tokenLogo.setLogo(getValue(mapping.logo()));
        return tokenLogo;
    }


    private static String getValue(Item item) {
        return getValue(item, Function.identity());
    }

    private static <T> T getValue(Item item, Function<String, T> f) {
        return Optional.ofNullable(item).map(Item::value).map(f).orElse(null);
    }


}
