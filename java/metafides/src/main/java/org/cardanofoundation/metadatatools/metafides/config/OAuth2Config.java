package org.cardanofoundation.metadatatools.metafides.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
@NoArgsConstructor
@ConfigurationProperties(prefix = "oauth2")
public class OAuth2Config {
    private String issuerUri;
    private String keyStoreUri;
    private String[] basicAuthPaths;
    private String[] permissionlessPaths;
    private String baseUrl;
    private Integer requestTimeout;
}
