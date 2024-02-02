package org.cardanofoundation.tokenmetadata.registry.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Mapping(String subject,
                      Item url,
                      Item name,
                      Item ticker,
                      Item decimals,
                      Item logo,
                      String policy,
                      Item description) {
}
