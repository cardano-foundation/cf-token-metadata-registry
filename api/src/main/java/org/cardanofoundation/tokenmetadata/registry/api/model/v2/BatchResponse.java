package org.cardanofoundation.tokenmetadata.registry.api.model.v2;

import java.util.List;

public record BatchResponse(List<Subject> subjects, List<String> queryPriority) {
}
