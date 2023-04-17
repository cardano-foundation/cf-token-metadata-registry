package org.cardanofoundation.metadatatools.api.model.rest.wellknownproperties;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.cardanofoundation.metadatatools.api.model.rest.TokenMetadataProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

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
