package org.cardanofoundation.metadatatools.metafides;

import lombok.extern.log4j.Log4j2;
import org.bouncycastle.util.encoders.Base64;
import org.cardanofoundation.metadatatools.metafides.data.OAuth2GrantType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;


@Log4j2
@Component
public class AuthenticationInterceptor implements HandlerInterceptor {

    private static final String[] BASIC_AUTH_PATHS = {"/oauth2/token"};
    private static final String[] PERMISSIONLESS_PATHS = {"/health"};

    @Override
    public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object object) throws Exception {
        final String authHeader = request.getHeader("authorization");
        final String servletPath = request.getServletPath();
        if (authHeader != null) {
            if (authHeader.toLowerCase().startsWith("basic")) {
                if (Arrays.stream(BASIC_AUTH_PATHS).anyMatch(servletPath::equalsIgnoreCase)) {
                    final String decodedCredentials = new String(Base64.decode(authHeader.substring("basic".length()).trim()), StandardCharsets.UTF_8);
                    final String[] credentialComponents = decodedCredentials.split(":", 2);
                    if (credentialComponents.length == 2) {
                        // fetch secrets and make request to OID provider for bearer token and clientid
                        final String grantTypeParameter = request.getParameter("grant_type");
                        final OAuth2GrantType grantType = OAuth2GrantType.valueOf(request.getParameter("grant_type").toUpperCase());
                        request.setAttribute("accessToken", credentialComponents[1]);
                        return true;
                    }
                }
            } else if (authHeader.toLowerCase().startsWith("bearer")) {
                // fetch bearer token content, parse and ask idp if that one is valid
                return true;
            }
        } else {
            if (Arrays.stream(PERMISSIONLESS_PATHS).anyMatch(servletPath::equalsIgnoreCase)) {
                return true;
            }
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No valid credentials provided.");
    }
}
