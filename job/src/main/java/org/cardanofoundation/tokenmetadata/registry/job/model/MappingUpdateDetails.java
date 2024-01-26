package org.cardanofoundation.tokenmetadata.registry.job.model;

import java.time.LocalDateTime;

public record MappingUpdateDetails(String updatedBy, LocalDateTime updatedAt) {
}
