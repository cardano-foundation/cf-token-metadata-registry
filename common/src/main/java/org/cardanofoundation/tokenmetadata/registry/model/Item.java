package org.cardanofoundation.tokenmetadata.registry.model;

import java.util.List;

public record Item(Integer sequenceNumber, String value, List<Signature> signatures) {
}
