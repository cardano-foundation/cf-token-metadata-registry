package org.cardanofoundation.tokenmetadata.registry.api.model.rest.wellknownproperties;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import org.cardanofoundation.tokenmetadata.registry.api.model.rest.TokenMetadataProperty;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class LogoProperty extends TokenMetadataProperty<byte[]> {
  @Schema(name = "value", required = true)
  public byte[] getValue() {
    return super.getValue();
  }
}
