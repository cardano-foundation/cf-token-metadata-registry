package org.cardanofoundation.metadatatools.api.model.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchResponse {
  @JsonProperty("subjects")
  @Valid
  @Schema(name = "subjects", required = true)
  private List<TokenMetadata> subjects = new ArrayList<>();
}
