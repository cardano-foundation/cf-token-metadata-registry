package org.cardanofoundation.tokenmetadata.registry.api.model.v2;

public record StringProperty(String value, String source) implements Property<String> {
}
