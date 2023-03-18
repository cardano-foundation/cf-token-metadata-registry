package org.cardanofoundation.metadatatools.api.model.rest.wellknownproperties;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import org.cardanofoundation.metadatatools.api.model.rest.TokenMetadataProperty;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class LogoProperty extends TokenMetadataProperty<String> {
  @Schema(name = "value", required = true)
  public String getValue() {
    return super.getValue();
  }
}

