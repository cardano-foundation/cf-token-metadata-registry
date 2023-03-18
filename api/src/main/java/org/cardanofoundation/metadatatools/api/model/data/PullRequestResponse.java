package org.cardanofoundation.metadatatools.api.model.data;

public class PullRequestResponse {
    private String state;
    private boolean merged;


    public PullRequestResponse() {
    }

    public PullRequestResponse(String state, boolean merged) {
        this.state = state;
        this.merged = merged;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public boolean isMerged() {
        return merged;
    }

    public void setMerged(boolean merged) {
        this.merged = merged;
    }
}
