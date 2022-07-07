package org.cardanofoundation.metadatatools.api.model.rest.wellknownproperties;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.cardanofoundation.metadatatools.api.model.rest.TokenMetadataProperty;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class DecimalsProperty extends TokenMetadataProperty<BigDecimal> {
  @Valid @DecimalMin("0") @DecimalMax("255")
  @Schema(name = "value", example = "1", required = true)
  public BigDecimal getValue() {
    return super.getValue();
  }
}
