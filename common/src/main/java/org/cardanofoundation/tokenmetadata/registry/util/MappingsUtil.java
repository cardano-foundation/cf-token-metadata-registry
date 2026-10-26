package org.cardanofoundation.tokenmetadata.registry.util;

import org.cardanofoundation.tokenmetadata.registry.entity.TokenLogo;
import org.cardanofoundation.tokenmetadata.registry.entity.TokenMetadata;
import org.cardanofoundation.tokenmetadata.registry.model.Item;
import org.cardanofoundation.tokenmetadata.registry.model.Mapping;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

@Slf4j
public final class MappingsUtil {

    private MappingsUtil() {
    }

    public static TokenMetadata toTokenMetadata(Mapping mapping, String updateBy, LocalDateTime updatedAt) {
        TokenMetadata tokenMetadata = new TokenMetadata();
        tokenMetadata.setSubject(normalizeSubject(mapping.subject()));
        tokenMetadata.setPolicy(mapping.policy());
        tokenMetadata.setName(getValue(mapping.name()));
        tokenMetadata.setTicker(getValue(mapping.ticker()));
        tokenMetadata.setUrl(getValue(mapping.url()));
        tokenMetadata.setDescription(getValue(mapping.description()));
        tokenMetadata.setDecimals(getValue(mapping.decimals(), s -> {
            try {
                return Long.valueOf(s);
            } catch (NumberFormatException _) {
                log.warn("Invalid decimals value '{}' for subject '{}'", s, mapping.subject());
                return null;
            }
        }));
        tokenMetadata.setUpdated(updatedAt);
        tokenMetadata.setUpdatedBy(updateBy);
        tokenMetadata.setProperties(mapping);
        return tokenMetadata;
    }

    /**
     * A subject as it is stored and looked up: lower case. A subject is base16, so its case carries no meaning, and
     * CIP-68 subjects and the asset units clients query with are lower case; a registry file named in upper case would
     * otherwise only be found by a query in upper case.
     */
    @Nullable
    public static String normalizeSubject(@Nullable String subject) {
        return subject == null ? null : subject.toLowerCase(Locale.ROOT);
    }

    public static TokenLogo toTokenLogo(Mapping mapping) {
        TokenLogo tokenLogo = new TokenLogo();
        tokenLogo.setSubject(normalizeSubject(mapping.subject()));
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
