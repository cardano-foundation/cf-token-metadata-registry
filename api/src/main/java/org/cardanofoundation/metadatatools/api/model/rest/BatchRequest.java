package org.cardanofoundation.metadatatools.api.model.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchRequest {

  @JsonProperty("subjects")
  @Valid
  @Schema(name = "subjects", required = true)
  private List<String> subjects = new ArrayList<>();

  @JsonProperty("properties")
  @Valid
  @Schema(name = "properties", required = false)
  private List<String> properties = null;
}
