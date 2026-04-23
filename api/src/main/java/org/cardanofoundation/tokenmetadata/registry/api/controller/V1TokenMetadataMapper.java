package org.cardanofoundation.tokenmetadata.registry.api.controller;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.Item;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.Mapping;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.Signature;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model.TokenMetadata;
import jakarta.annotation.Nullable;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.AnnotatedSignature;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.TokenMetadataProperty;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.wellknownproperties.DecimalsProperty;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.wellknownproperties.DescriptionProperty;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.wellknownproperties.LogoProperty;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.wellknownproperties.NameProperty;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.wellknownproperties.TickerProperty;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.wellknownproperties.UrlProperty;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Maps assets-ext {@code ft_offchain_metadata} entities onto the V1 CIP-26 annotated-property
 * response shape ({@code {value, signatures, sequenceNumber}}).
 */
final class V1TokenMetadataMapper {

    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";
    private static final String URL = "url";
    private static final String TICKER = "ticker";
    private static final String DECIMALS = "decimals";
    private static final String LOGO = "logo";

    private V1TokenMetadataMapper() {
    }

    static org.cardanofoundation.tokenmetadata.registry.api.model.rest.TokenMetadata toDto(
            TokenMetadata entity,
            @Nullable Collection<String> propertyFilter) {

        org.cardanofoundation.tokenmetadata.registry.api.model.rest.TokenMetadata dto =
                new org.cardanofoundation.tokenmetadata.registry.api.model.rest.TokenMetadata();
        dto.setSubject(entity.getSubject());
        dto.setPolicy(entity.getPolicy());
        dto.setUpdated(entity.getUpdated() != null
                ? Date.from(entity.getUpdated().atZone(java.time.ZoneId.systemDefault()).toInstant())
                : null);
        dto.setUpdatedBy(entity.getUpdatedBy());

        Mapping mapping = entity.getProperties();
        if (mapping == null) {
            return dto;
        }

        if (includes(propertyFilter, NAME)) {
            toProperty(mapping.name(), NameProperty::new, Function.identity()).ifPresent(dto::setName);
        }
        if (includes(propertyFilter, DESCRIPTION)) {
            toProperty(mapping.description(), DescriptionProperty::new, Function.identity()).ifPresent(dto::setDescription);
        }
        if (includes(propertyFilter, URL)) {
            toProperty(mapping.url(), UrlProperty::new, Function.identity()).ifPresent(dto::setUrl);
        }
        if (includes(propertyFilter, TICKER)) {
            toProperty(mapping.ticker(), TickerProperty::new, Function.identity()).ifPresent(dto::setTicker);
        }
        if (includes(propertyFilter, DECIMALS)) {
            toProperty(mapping.decimals(), DecimalsProperty::new, V1TokenMetadataMapper::parseDecimals)
                    .ifPresent(dto::setDecimals);
        }
        if (includes(propertyFilter, LOGO)) {
            toProperty(mapping.logo(), LogoProperty::new, V1TokenMetadataMapper::decodeBase64)
                    .ifPresent(dto::setLogo);
        }

        return dto;
    }

    private static boolean includes(@Nullable Collection<String> filter, String name) {
        return filter == null || filter.isEmpty() || filter.contains(name);
    }

    private static <T, P extends TokenMetadataProperty<T>> Optional<P> toProperty(
            @Nullable Item item,
            java.util.function.Supplier<P> factory,
            Function<String, T> valueMapper) {

        if (item == null || item.value() == null) {
            return Optional.empty();
        }
        P property = factory.get();
        property.setValue(valueMapper.apply(item.value()));
        property.setSignatures(toSignatures(item.signatures()));
        property.setSequenceNumber(item.sequenceNumber() != null
                ? BigDecimal.valueOf(item.sequenceNumber()) : BigDecimal.ZERO);
        return Optional.of(property);
    }

    private static List<AnnotatedSignature> toSignatures(@Nullable List<Signature> signatures) {
        if (signatures == null || signatures.isEmpty()) {
            return Collections.emptyList();
        }
        return signatures.stream()
                .map(s -> new AnnotatedSignature(s.signature(), s.publicKey()))
                .toList();
    }

    private static BigDecimal parseDecimals(String raw) {
        try {
            return new BigDecimal(raw);
        } catch (NumberFormatException _) {
            return null;
        }
    }

    private static byte[] decodeBase64(String raw) {
        return Base64.getDecoder().decode(raw);
    }

}
