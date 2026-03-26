package org.cardanofoundation.tokenmetadata.registry.api.model.v2;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Marker interface for V2 API extensions.
 * Each CIP standard that enriches the Subject response implements this interface.
 * Extensions are serialized into the {@code extensions} map keyed by their CIP identifier.
 */
@Schema(description = "Base type for CIP extensions. See specific implementations like ProgrammableTokenCip113.")
public interface Extension {
}
