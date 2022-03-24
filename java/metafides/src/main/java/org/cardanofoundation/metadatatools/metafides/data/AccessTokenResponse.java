package org.cardanofoundation.metadatatools.metafides.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AccessTokenResponse {
    public enum TokenType {
        @JsonProperty("bearer")
        BEARER;
    }

    @JsonProperty("token_type")
    private TokenType tokenType;

    @JsonProperty("access_token")
    private String accessToken;
}
