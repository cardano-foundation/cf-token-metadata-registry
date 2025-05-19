package org.cardanofoundation.tokenmetadata.registry.api.model.v2;

public record LongProperty(Long value, String source) implements Property<Long> {
}
