package org.cardanofoundation.tokenmetadata.registry.api.model.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletTrustCheckResponse {
  @JsonProperty("incidents")
  @Valid
  @Schema(name = "incidents", required = true)
  private List<WalletFraudIncident> incidents = new ArrayList<>();
}
