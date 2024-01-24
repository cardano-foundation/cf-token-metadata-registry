package org.cardanofoundation.tokenmetadata.registry.api.model.rest.wellknownproperties;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.TokenMetadataProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class TickerProperty extends TokenMetadataProperty<String> {
  @Valid
  @Size(min = 2, max = 9)
  @Schema(name = "value", example = "QUID", required = true)
  public String getValue() {
    return super.getValue();
  }
}
