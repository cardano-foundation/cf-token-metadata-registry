package org.cardanofoundation.tokenmetadata.registry.api.model.rest.wellknownproperties;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.TokenMetadataProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class UrlProperty extends TokenMetadataProperty<String> {
  @Valid
  @Pattern(regexp = "^https://")
  @Size(max = 250)
  @Schema(name = "value", example = "https://www.iohk.io", required = true)
  public String getValue() {
    return super.getValue();
  }
}
