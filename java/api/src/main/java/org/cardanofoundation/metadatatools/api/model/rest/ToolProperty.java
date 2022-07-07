package org.cardanofoundation.metadatatools.api.model.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.Objects;

/**
 * ToolProperty
 */
public class ToolProperty   {

  @JsonProperty("url")
  private String url;

  @JsonProperty("description")
  private String description;

  public ToolProperty url(String url) {
    this.url = url;
    return this;
  }

  /**
   * Get url
   * @return url
  */
  @Pattern(regexp = "^https://") @Size(max = 250) 
  @Schema(name = "url", example = "https://www.iohk.io", required = false)
  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public ToolProperty description(String description) {
    this.description = description;
    return this;
  }

  /**
   * Get description
   * @return description
  */
  @Size(max = 500) 
  @Schema(name = "description", required = false)
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ToolProperty toolProperty = (ToolProperty) o;
    return Objects.equals(this.url, toolProperty.url) &&
        Objects.equals(this.description, toolProperty.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(url, description);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ToolProperty {\n");
    sb.append("    url: ").append(toIndentedString(url)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
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

