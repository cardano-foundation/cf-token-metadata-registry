package org.cardanofoundation.tokenmetadata.registry.api.model.v2;

import java.util.List;

public record Response(Subject subject, List<String> queryPriority) {
}
