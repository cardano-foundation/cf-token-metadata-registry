package org.cardanofoundation.tokenmetadata.registry.service;

import lombok.Builder;
import lombok.Data;
import org.cardanofoundation.tokenmetadata.registry.model.enums.SyncStatusEnum;

@Builder
@Data
public class SyncStatus {

    private boolean isInitialSyncDone;
    private SyncStatusEnum syncStatus;

}
