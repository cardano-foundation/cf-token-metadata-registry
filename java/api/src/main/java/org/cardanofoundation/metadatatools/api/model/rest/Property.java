package org.cardanofoundation.metadatatools.api.model.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.Objects;

/**
 * Property
 */
public class Property   {

  @JsonProperty("subject")
  private String subject;

  @JsonProperty("policy")
  private String policy;

  @JsonProperty("name")
  private NameProperty name;

  @JsonProperty("description")
  private DescriptionProperty description;

  @JsonProperty("url")
  private UrlProperty url;

  @JsonProperty("ticker")
  private TickerProperty ticker;

  @JsonProperty("decimals")
  private DecimalsProperty decimals;

  @JsonProperty("logo")
  private LogoProperty logo;

  @JsonProperty("tool")
  private ToolProperty tool;

  @JsonProperty("updated")
  private Date updated;

  @JsonProperty("updatedBy")
  private String updatedBy;

  public Property subject(String subject) {
    this.subject = subject;
    return this;
  }

  /**
   * Get subject
   * @return subject
  */
  @NotNull 
  @Schema(name = "subject", example = "b377d03a568dde663534e040fc32a57323ec00970df0e863eba3f098717569640a", required = true)
  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public Property policy(String policy) {
    this.policy = policy;
    return this;
  }

  /**
   * Get policy
   * @return policy
  */
  
  @Schema(name = "policy", example = "82008200581ce62601e8eeec975f3f124a288cd0ecb2973f5fc225629f1401a79b16", required = false)
  public String getPolicy() {
    return policy;
  }

  public void setPolicy(String policy) {
    this.policy = policy;
  }

  public Property name(NameProperty name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
  */
  @Valid 
  @Schema(name = "name", required = false)
  public NameProperty getName() {
    return name;
  }

  public void setName(NameProperty name) {
    this.name = name;
  }

  public Property description(DescriptionProperty description) {
    this.description = description;
    return this;
  }

  /**
   * Get description
   * @return description
  */
  @Valid 
  @Schema(name = "description", required = false)
  public DescriptionProperty getDescription() {
    return description;
  }

  public void setDescription(DescriptionProperty description) {
    this.description = description;
  }

  public Property url(UrlProperty url) {
    this.url = url;
    return this;
  }

  /**
   * Get url
   * @return url
  */
  @Valid 
  @Schema(name = "url", required = false)
  public UrlProperty getUrl() {
    return url;
  }

  public void setUrl(UrlProperty url) {
    this.url = url;
  }

  public Property ticker(TickerProperty ticker) {
    this.ticker = ticker;
    return this;
  }

  /**
   * Get ticker
   * @return ticker
  */
  @Valid 
  @Schema(name = "ticker", required = false)
  public TickerProperty getTicker() {
    return ticker;
  }

  public void setTicker(TickerProperty ticker) {
    this.ticker = ticker;
  }

  public Property decimals(DecimalsProperty decimals) {
    this.decimals = decimals;
    return this;
  }

  /**
   * Get decimals
   * @return decimals
  */
  @Valid 
  @Schema(name = "decimals", required = false)
  public DecimalsProperty getDecimals() {
    return decimals;
  }

  public void setDecimals(DecimalsProperty decimals) {
    this.decimals = decimals;
  }

  public Property logo(LogoProperty logo) {
    this.logo = logo;
    return this;
  }

  /**
   * Get logo
   * @return logo
  */
  @Valid 
  @Schema(name = "logo", required = false)
  public LogoProperty getLogo() {
    return logo;
  }

  public void setLogo(LogoProperty logo) {
    this.logo = logo;
  }

  public Property tool(ToolProperty tool) {
    this.tool = tool;
    return this;
  }

  /**
   * Get tool
   * @return tool
  */
  @Valid 
  @Schema(name = "tool", required = false)
  public ToolProperty getTool() {
    return tool;
  }

  public void setTool(ToolProperty tool) {
    this.tool = tool;
  }


  @Valid
  @Schema(name = "updated", required = false)
  public Date getUpdated() {
    return updated;
  }

  public void setUpdated(final Date updated) {
    this.updated = updated;
  }

  public Property updated(final Date updated) {
    this.updated = updated;
    return this;
  }

  @Valid
  @Schema(name = "updatedBy", required = false)
  public String getUpdatedBy() {
    return updatedBy;
  }

  public void setUpdatedBy(final String updatedBy) {
    this.updatedBy = updatedBy;
  }

  public Property updatedBy(final String updatedBy) {
    this.updatedBy = updatedBy;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Property property = (Property) o;
    return Objects.equals(this.subject, property.subject) &&
        Objects.equals(this.policy, property.policy) &&
        Objects.equals(this.name, property.name) &&
        Objects.equals(this.description, property.description) &&
        Objects.equals(this.url, property.url) &&
        Objects.equals(this.ticker, property.ticker) &&
        Objects.equals(this.decimals, property.decimals) &&
        Objects.equals(this.logo, property.logo) &&
        Objects.equals(this.tool, property.tool) &&
        Objects.equals(this.updated, property.updated) &&
        Objects.equals(this.updatedBy, property.updatedBy);
  }

  @Override
  public int hashCode() {
    return Objects.hash(subject, policy, name, description, url, ticker, decimals, logo, tool);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("class Property {\n");
    sb.append("    subject: ").append(toIndentedString(subject)).append("\n");
    sb.append("    policy: ").append(toIndentedString(policy)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    url: ").append(toIndentedString(url)).append("\n");
    sb.append("    ticker: ").append(toIndentedString(ticker)).append("\n");
    sb.append("    decimals: ").append(toIndentedString(decimals)).append("\n");
    sb.append("    logo: ").append(toIndentedString(logo)).append("\n");
    sb.append("    tool: ").append(toIndentedString(tool)).append("\n");
    sb.append("    updated: ").append(toIndentedString(updated)).append("\n");
    sb.append("    updatedBy: ").append(toIndentedString(updatedBy)).append("\n");
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

