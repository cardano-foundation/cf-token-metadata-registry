package org.cardanofoundation.metadatatools.api.model.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenMetadataProperty<T> {
  @JsonProperty("signatures")
  @Valid
  @Schema(name = "signatures", required = true)
  private List<AnnotatedSignature> signatures = new ArrayList<>();

  @JsonProperty("sequenceNumber")
  @Valid
  @DecimalMin("0")
  @Schema(name = "sequenceNumber", required = true)
  private BigDecimal sequenceNumber;

  @JsonProperty("value")
  private T value;
}
