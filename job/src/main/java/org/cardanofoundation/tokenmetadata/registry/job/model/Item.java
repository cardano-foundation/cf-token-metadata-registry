package org.cardanofoundation.tokenmetadata.registry.job.model;

import java.util.List;

public record Item(Integer sequenceNumber, String value, List<Signature> signatures) {
}
