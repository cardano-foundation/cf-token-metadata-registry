package org.cardanofoundation.metadatatools.api.model.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.Valid;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletFraudIncident {
  @JsonProperty("addressHash")
  @Schema(
      name = "addressHash",
      example = "b377d03a568dde663534e040fc32a57323ec00970df0e863eba3f098717569640a",
      required = true)
  private String addressHash;

  @JsonProperty("incidentId")
  @Schema(name = "incidentId", required = true)
  private Integer incidentId;

  @JsonProperty("scamSiteDomain")
  @Schema(name = "scamSiteDomain", example = "adagain.com", required = true)
  private String scamSiteDomain;

  @JsonProperty("reportedDate")
  @Valid
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  @Schema(name = "reportedDate", required = true)
  private LocalDate reportedDate;
}
