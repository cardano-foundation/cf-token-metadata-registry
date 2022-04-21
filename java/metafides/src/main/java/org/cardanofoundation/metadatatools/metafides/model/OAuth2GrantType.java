package org.cardanofoundation.metadatatools.metafides.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

public enum OAuth2GrantType {
    @JsonProperty("client_credentials")
    CLIENT_CREDENTIALS("client_credentials");

    private final String value;

    OAuth2GrantType(final String value) {
        this.value = value;
    }

    final public String getValue() {
        return this.value;
    }
}
