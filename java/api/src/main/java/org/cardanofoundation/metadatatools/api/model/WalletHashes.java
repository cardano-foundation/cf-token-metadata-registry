package org.cardanofoundation.metadatatools.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * WalletHashes
 */
public class WalletHashes   {

  @JsonProperty("addressHashes")
  @Valid
  private List<String> addressHashes = new ArrayList<>();

  public WalletHashes addressHashes(List<String> addressHashes) {
    this.addressHashes = addressHashes;
    return this;
  }

  public WalletHashes addAddressHashesItem(String addressHashesItem) {
    if (this.addressHashes == null) {
      this.addressHashes = new ArrayList<>();
    }
    this.addressHashes.add(addressHashesItem);
    return this;
  }

  /**
   * Get addressHashes
   * @return addressHashes
  */
  @NotNull 
  @Schema(name = "addressHashes", required = true)
  public List<String> getAddressHashes() {
    return addressHashes;
  }

  public void setAddressHashes(List<String> addressHashes) {
    this.addressHashes = addressHashes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WalletHashes walletHashes = (WalletHashes) o;
    return Objects.equals(this.addressHashes, walletHashes.addressHashes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(addressHashes);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WalletHashes {\n");
    sb.append("    addressHashes: ").append(toIndentedString(addressHashes)).append("\n");
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

