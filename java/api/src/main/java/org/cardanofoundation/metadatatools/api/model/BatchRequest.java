package org.cardanofoundation.metadatatools.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * BatchRequest
 */
public class BatchRequest   {

  @JsonProperty("subjects")
  @Valid
  private List<String> subjects = new ArrayList<>();

  @JsonProperty("properties")
  @Valid
  private List<String> properties = null;

  public BatchRequest subjects(List<String> subjects) {
    this.subjects = subjects;
    return this;
  }

  public BatchRequest addSubjectsItem(String subjectsItem) {
    if (this.subjects == null) {
      this.subjects = new ArrayList<>();
    }
    this.subjects.add(subjectsItem);
    return this;
  }

  /**
   * Get subjects
   * @return subjects
  */
  @NotNull 
  @Schema(name = "subjects", required = true)
  public List<String> getSubjects() {
    return subjects;
  }

  public void setSubjects(List<String> subjects) {
    this.subjects = subjects;
  }

  public BatchRequest properties(List<String> properties) {
    this.properties = properties;
    return this;
  }

  public BatchRequest addPropertiesItem(String propertiesItem) {
    if (this.properties == null) {
      this.properties = new ArrayList<>();
    }
    this.properties.add(propertiesItem);
    return this;
  }

  /**
   * Get properties
   * @return properties
  */
  
  @Schema(name = "properties", required = false)
  public List<String> getProperties() {
    return properties;
  }

  public void setProperties(List<String> properties) {
    this.properties = properties;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BatchRequest batchRequest = (BatchRequest) o;
    return Objects.equals(this.subjects, batchRequest.subjects) &&
        Objects.equals(this.properties, batchRequest.properties);
  }

  @Override
  public int hashCode() {
    return Objects.hash(subjects, properties);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BatchRequest {\n");
    sb.append("    subjects: ").append(toIndentedString(subjects)).append("\n");
    sb.append("    properties: ").append(toIndentedString(properties)).append("\n");
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

