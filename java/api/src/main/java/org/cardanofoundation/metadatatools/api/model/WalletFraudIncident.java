package org.cardanofoundation.metadatatools.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Objects;

/**
 * WalletFraudIncident
 */
public class WalletFraudIncident   {

  @JsonProperty("addressHash")
  private String addressHash;

  @JsonProperty("incidentId")
  private Integer incidentId;

  @JsonProperty("scamSiteDomain")
  private String scamSiteDomain;

  @JsonProperty("reportedDate")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate reportedDate;

  public WalletFraudIncident addressHash(String addressHash) {
    this.addressHash = addressHash;
    return this;
  }

  /**
   * Get addressHash
   * @return addressHash
  */
  @NotNull 
  @Schema(name = "addressHash", example = "b377d03a568dde663534e040fc32a57323ec00970df0e863eba3f098717569640a", required = true)
  public String getAddressHash() {
    return addressHash;
  }

  public void setAddressHash(String addressHash) {
    this.addressHash = addressHash;
  }

  public WalletFraudIncident incidentId(Integer incidentId) {
    this.incidentId = incidentId;
    return this;
  }

  /**
   * Get incidentId
   * @return incidentId
  */
  @NotNull 
  @Schema(name = "incidentId", required = true)
  public Integer getIncidentId() {
    return incidentId;
  }

  public void setIncidentId(Integer incidentId) {
    this.incidentId = incidentId;
  }

  public WalletFraudIncident scamSiteDomain(String scamSiteDomain) {
    this.scamSiteDomain = scamSiteDomain;
    return this;
  }

  /**
   * Get scamSiteDomain
   * @return scamSiteDomain
  */
  @NotNull 
  @Schema(name = "scamSiteDomain", example = "adagain.com", required = true)
  public String getScamSiteDomain() {
    return scamSiteDomain;
  }

  public void setScamSiteDomain(String scamSiteDomain) {
    this.scamSiteDomain = scamSiteDomain;
  }

  public WalletFraudIncident reportedDate(LocalDate reportedDate) {
    this.reportedDate = reportedDate;
    return this;
  }

  /**
   * Get reportedDate
   * @return reportedDate
  */
  @NotNull @Valid 
  @Schema(name = "reportedDate", required = true)
  public LocalDate getReportedDate() {
    return reportedDate;
  }

  public void setReportedDate(LocalDate reportedDate) {
    this.reportedDate = reportedDate;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WalletFraudIncident walletFraudIncident = (WalletFraudIncident) o;
    return Objects.equals(this.addressHash, walletFraudIncident.addressHash) &&
        Objects.equals(this.incidentId, walletFraudIncident.incidentId) &&
        Objects.equals(this.scamSiteDomain, walletFraudIncident.scamSiteDomain) &&
        Objects.equals(this.reportedDate, walletFraudIncident.reportedDate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(addressHash, incidentId, scamSiteDomain, reportedDate);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WalletFraudIncident {\n");
    sb.append("    addressHash: ").append(toIndentedString(addressHash)).append("\n");
    sb.append("    incidentId: ").append(toIndentedString(incidentId)).append("\n");
    sb.append("    scamSiteDomain: ").append(toIndentedString(scamSiteDomain)).append("\n");
    sb.append("    reportedDate: ").append(toIndentedString(reportedDate)).append("\n");
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

