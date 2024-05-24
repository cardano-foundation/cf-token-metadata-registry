package org.cardanofoundation.tokenmetadata.registry.model.enums;

import org.cardanofoundation.tokenmetadata.registry.util.Constants;

public enum SyncStatusEnum {

    SYNC_NOT_STARTED(Constants.SYNC_NOT_STARTED),
    SYNC_IN_PROGRESS(Constants.SYNC_IN_PROGRESS),
    SYNC_DONE(Constants.SYNC_DONE),
    SYNC_ERROR(Constants.SYNC_ERROR);

    private final String text;

    SyncStatusEnum(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
