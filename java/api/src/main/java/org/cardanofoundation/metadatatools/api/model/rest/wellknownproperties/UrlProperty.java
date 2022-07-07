package org.cardanofoundation.metadatatools.api.model.rest.wellknownproperties;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.cardanofoundation.metadatatools.api.model.rest.TokenMetadataProperty;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class UrlProperty extends TokenMetadataProperty<String> {
  @Valid @Pattern(regexp = "^https://") @Size(max = 250)
  @Schema(name = "value", example = "https://www.iohk.io", required = true)
  public String getValue() {
    return super.getValue();
  }
}
