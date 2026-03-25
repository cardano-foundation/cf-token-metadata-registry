package org.cardanofoundation.tokenmetadata.registry.api.model.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnnotatedSignature {

  @JsonProperty("signature")
  @Schema(name = "signature", requiredMode = Schema.RequiredMode.REQUIRED)
  private String signature;

  @JsonProperty("publicKey")
  @Schema(name = "publicKey", requiredMode = Schema.RequiredMode.REQUIRED)
  private String publicKey;
}
