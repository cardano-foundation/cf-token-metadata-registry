package org.cardanofoundation.tokenmetadata.registry.api.model.v2;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * The V2 batch response. Its schema has its own name: springdoc names a schema after the simple class name, and the V1
 * response is also a {@code BatchResponse}, so both endpoints would refer to one schema.
 */
@Schema(name = "SubjectBatchResponse", description = "The subjects found, in the order asked, and the query priority used")
public record BatchResponse(List<Subject> subjects,
                            List<String> queryPriority) {
}
