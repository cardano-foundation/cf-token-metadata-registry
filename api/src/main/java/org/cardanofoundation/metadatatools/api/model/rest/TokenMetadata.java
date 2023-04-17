package org.cardanofoundation.metadatatools.api.model.rest;

import com.fasterxml.jackson.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.cardanofoundation.metadatatools.api.model.rest.wellknownproperties.*;

import jakarta.validation.Valid;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@Log4j2
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TokenMetadata {

  @JsonProperty("subject")
  @Schema(
      name = "subject",
      example = "b377d03a568dde663534e040fc32a57323ec00970df0e863eba3f098717569640a",
      required = true)
  private String subject;

  @JsonProperty("policy")
  @Schema(
      name = "policy",
      example = "82008200581ce62601e8eeec975f3f124a288cd0ecb2973f5fc225629f1401a79b16")
  private String policy;

  @JsonProperty("name")
  @Valid
  @Schema(name = "name", required = true)
  private NameProperty name;

  @JsonProperty("description")
  @Valid
  @Schema(name = "description", required = true)
  private DescriptionProperty description;

  @JsonProperty("url")
  @Valid
  @Schema(name = "url")
  private UrlProperty url;

  @JsonProperty("ticker")
  @Valid
  @Schema(name = "ticker")
  private TickerProperty ticker;

  @JsonProperty("decimals")
  @Valid
  @Schema(name = "decimals")
  private DecimalsProperty decimals;

  @JsonProperty("logo")
  @Valid
  @Schema(name = "logo")
  private LogoProperty logo;

  @JsonProperty("updated")
  @Valid
  @Schema(name = "updated")
  private Date updated;

  @JsonProperty("updatedBy")
  @Valid
  @Schema(name = "updatedBy")
  private String updatedBy;

  private Map<String, TokenMetadataProperty<?>> additionalProperties = new HashMap<>();

  @JsonAnySetter
  public void propertiesSetter(final String propertyName, final TokenMetadataProperty<?> property) {
    if (propertyName == null) {
      throw new IllegalArgumentException("propertyName cannot be null.");
    }

    final String propertyNameSanitized = sanitizePropertyName(propertyName);
    if (propertyNameSanitized.isEmpty()) {
      throw new IllegalArgumentException("propertyName cannot be empty or blank.");
    }

    if (property != null) {
      this.additionalProperties.put(propertyNameSanitized, property);
    } else {
      this.additionalProperties.remove(propertyNameSanitized);
    }
  }

  @JsonAnyGetter
  public Map<String, TokenMetadataProperty<?>> propertiesGetter() {
    return getAdditionalProperties();
  }

  public void removeProperty(final String propertyName) {
    if (propertyName == null) {
      throw new IllegalArgumentException("propertyName cannot be null.");
    }

    final String propertyNameSanitized = sanitizePropertyName(propertyName);
    if (propertyNameSanitized.isEmpty()) {
      throw new IllegalArgumentException("propertyName cannot be empty or blank.");
    }

    this.additionalProperties.remove(propertyNameSanitized);
  }

  private static String sanitizePropertyName(final String propertyName) {
    if (propertyName == null) {
      throw new IllegalArgumentException("propertyName cannot be null.");
    }
    return propertyName.trim();
  }
}
