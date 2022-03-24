package org.cardanofoundation.metadatatools.metafides.data;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum OAuth2GrantType {
    @JsonProperty("client_credentials")
    CLIENT_CREDENTIALS;
}
