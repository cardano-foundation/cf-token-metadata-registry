package org.cardanofoundation.metadatatools.api.model.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * WalletTrustCheckResponse
 */
public class WalletTrustCheckResponse   {

  @JsonProperty("incidents")
  @Valid
  private List<WalletFraudIncident> incidents = new ArrayList<>();

  public WalletTrustCheckResponse incidents(List<WalletFraudIncident> incidents) {
    this.incidents = incidents;
    return this;
  }

  public WalletTrustCheckResponse addIncidentsItem(WalletFraudIncident incidentsItem) {
    if (this.incidents == null) {
      this.incidents = new ArrayList<>();
    }
    this.incidents.add(incidentsItem);
    return this;
  }

  /**
   * Get incidents
   * @return incidents
  */
  @NotNull @Valid 
  @Schema(name = "incidents", required = true)
  public List<WalletFraudIncident> getIncidents() {
    return incidents;
  }

  public void setIncidents(List<WalletFraudIncident> incidents) {
    this.incidents = incidents;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WalletTrustCheckResponse walletTrustCheckResponse = (WalletTrustCheckResponse) o;
    return Objects.equals(this.incidents, walletTrustCheckResponse.incidents);
  }

  @Override
  public int hashCode() {
    return Objects.hash(incidents);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WalletTrustCheckResponse {\n");
    sb.append("    incidents: ").append(toIndentedString(incidents)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

