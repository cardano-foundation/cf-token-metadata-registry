package org.cardanofoundation.metadatatools.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotNull;
import java.util.Objects;

/**
 * AnnotatedSignature
 */
public class AnnotatedSignature   {

  @JsonProperty("signature")
  private String signature;

  @JsonProperty("publicKey")
  private String publicKey;

  public AnnotatedSignature signature(String signature) {
    this.signature = signature;
    return this;
  }

  /**
   * Get signature
   * @return signature
  */
  @NotNull 
  @Schema(name = "signature", required = true)
  public String getSignature() {
    return signature;
  }

  public void setSignature(String signature) {
    this.signature = signature;
  }

  public AnnotatedSignature publicKey(String publicKey) {
    this.publicKey = publicKey;
    return this;
  }

  /**
   * Get publicKey
   * @return publicKey
  */
  @NotNull 
  @Schema(name = "publicKey", required = true)
  public String getPublicKey() {
    return publicKey;
  }

  public void setPublicKey(String publicKey) {
    this.publicKey = publicKey;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AnnotatedSignature annotatedSignature = (AnnotatedSignature) o;
    return Objects.equals(this.signature, annotatedSignature.signature) &&
        Objects.equals(this.publicKey, annotatedSignature.publicKey);
  }

  @Override
  public int hashCode() {
    return Objects.hash(signature, publicKey);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AnnotatedSignature {\n");
    sb.append("    signature: ").append(toIndentedString(signature)).append("\n");
    sb.append("    publicKey: ").append(toIndentedString(publicKey)).append("\n");
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

