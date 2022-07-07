package org.cardanofoundation.metadatatools.api.model.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * BatchResponse
 */
public class BatchResponse   {

  @JsonProperty("subjects")
  @Valid
  private List<Property> subjects = new ArrayList<>();

  public BatchResponse subjects(List<Property> subjects) {
    this.subjects = subjects;
    return this;
  }

  public BatchResponse addSubjectsItem(Property subjectsItem) {
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
  @NotNull @Valid 
  @Schema(name = "subjects", required = true)
  public List<Property> getSubjects() {
    return subjects;
  }

  public void setSubjects(List<Property> subjects) {
    this.subjects = subjects;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BatchResponse batchResponse = (BatchResponse) o;
    return Objects.equals(this.subjects, batchResponse.subjects);
  }

  @Override
  public int hashCode() {
    return Objects.hash(subjects);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BatchResponse {\n");
    sb.append("    subjects: ").append(toIndentedString(subjects)).append("\n");
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

