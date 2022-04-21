package org.cardanofoundation.metadatatools.metafides.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessTokenResponse {
    public enum TokenType {
        @JsonProperty("Bearer")
        BEARER;
    }

    @JsonProperty("token_type")
    private TokenType tokenType;

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("expires_in")
    private Integer expiresIn;
}
