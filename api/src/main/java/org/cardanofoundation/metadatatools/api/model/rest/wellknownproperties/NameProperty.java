package org.cardanofoundation.metadatatools.api.model.rest.wellknownproperties;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.cardanofoundation.metadatatools.api.model.rest.TokenMetadataProperty;

import javax.validation.Valid;
import javax.validation.constraints.Size;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class NameProperty extends TokenMetadataProperty<String> {
  @Valid
  @Size(min = 1, max = 50)
  @Schema(name = "value", required = true)
  public String getValue() {
    return super.getValue();
  }
}
