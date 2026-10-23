package org.cardanofoundation.tokenmetadata.registry.api.model.v2;

import io.swagger.v3.oas.annotations.media.Schema;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip113.ProgrammableTokenCip113;

/**
 * Marker interface for V2 API extensions.
 * Each CIP standard that enriches the Subject response implements this interface.
 * Extensions are serialized into the {@code extensions} map keyed by their CIP identifier.
 */
@Schema(description = "Base type for CIP extensions.",
        oneOf = {ProgrammableTokenCip113.class})
public interface Extension {
}
