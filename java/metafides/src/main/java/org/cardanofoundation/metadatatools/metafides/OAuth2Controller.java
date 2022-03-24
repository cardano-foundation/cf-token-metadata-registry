package org.cardanofoundation.metadatatools.metafides;

import org.cardanofoundation.metadatatools.metafides.data.AccessTokenResponse;
import org.cardanofoundation.metadatatools.metafides.data.OAuth2GrantType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/oauth2")
public class OAuth2Controller {
    @RequestMapping(value = "/token", method = RequestMethod.GET, produces = "application/json")
    public AccessTokenResponse getToken(@RequestAttribute("accessToken") final String accessToken, @RequestParam("grant_type") final OAuth2GrantType grantType) {
        return new AccessTokenResponse(AccessTokenResponse.TokenType.BEARER, accessToken);
    }
}
