package org.cardanofoundation.tokenmetadata.registry.api.model.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.cardanofoundation.tokenmetadata.registry.model.enums.SyncStatusEnum;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthResponse {

    @JsonProperty("synced")
    @Valid
    private boolean synced;

    @JsonProperty("syncStatus")
    @Valid
    private String syncStatus;

}
