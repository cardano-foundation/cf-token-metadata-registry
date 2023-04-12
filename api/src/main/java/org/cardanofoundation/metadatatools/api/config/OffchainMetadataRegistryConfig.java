package org.cardanofoundation.metadatatools.api.config;

import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
@NoArgsConstructor
@ConfigurationProperties(prefix = "cardano.offchain-metadata-registry")
public class OffchainMetadataRegistryConfig {
  private Map<String, String> networkSourceMappings;

  public boolean networkIsMapped(final String network) {
    return networkSourceMappings.containsKey(network);
  }

  public String sourceFromNetwork(final String network) throws IllegalArgumentException {
    if (networkSourceMappings.containsKey(network)) {
      return networkSourceMappings.get(network);
    } else {
      throw new IllegalArgumentException("Given network not supported.");
    }
  }
}
