package org.cardanofoundation.metadatatools.api.model.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * DecimalsProperty
 */
public class DecimalsProperty   {

  @JsonProperty("signatures")
  @Valid
  private List<AnnotatedSignature> signatures = new ArrayList<>();

  @JsonProperty("sequenceNumber")
  private BigDecimal sequenceNumber;

  @JsonProperty("value")
  private BigDecimal value;

  public DecimalsProperty signatures(List<AnnotatedSignature> signatures) {
    this.signatures = signatures;
    return this;
  }

  public DecimalsProperty addSignaturesItem(AnnotatedSignature signaturesItem) {
    if (this.signatures == null) {
      this.signatures = new ArrayList<>();
    }
    this.signatures.add(signaturesItem);
    return this;
  }

  /**
   * Get signatures
   * @return signatures
  */
  @NotNull @Valid 
  @Schema(name = "signatures", required = true)
  public List<AnnotatedSignature> getSignatures() {
    return signatures;
  }

  public void setSignatures(List<AnnotatedSignature> signatures) {
    this.signatures = signatures;
  }

  public DecimalsProperty sequenceNumber(BigDecimal sequenceNumber) {
    this.sequenceNumber = sequenceNumber;
    return this;
  }

  /**
   * Get sequenceNumber
   * minimum: 0
   * @return sequenceNumber
  */
  @NotNull @Valid @DecimalMin("0") 
  @Schema(name = "sequenceNumber", required = true)
  public BigDecimal getSequenceNumber() {
    return sequenceNumber;
  }

  public void setSequenceNumber(BigDecimal sequenceNumber) {
    this.sequenceNumber = sequenceNumber;
  }

  public DecimalsProperty value(BigDecimal value) {
    this.value = value;
    return this;
  }

  /**
   * Get value
   * minimum: 0
   * maximum: 255
   * @return value
  */
  @NotNull @Valid @DecimalMin("0") @DecimalMax("255") 
  @Schema(name = "value", example = "1", required = true)
  public BigDecimal getValue() {
    return value;
  }

  public void setValue(BigDecimal value) {
    this.value = value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DecimalsProperty decimalsProperty = (DecimalsProperty) o;
    return Objects.equals(this.signatures, decimalsProperty.signatures) &&
        Objects.equals(this.sequenceNumber, decimalsProperty.sequenceNumber) &&
        Objects.equals(this.value, decimalsProperty.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(signatures, sequenceNumber, value);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DecimalsProperty {\n");
    sb.append("    signatures: ").append(toIndentedString(signatures)).append("\n");
    sb.append("    sequenceNumber: ").append(toIndentedString(sequenceNumber)).append("\n");
    sb.append("    value: ").append(toIndentedString(value)).append("\n");
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

