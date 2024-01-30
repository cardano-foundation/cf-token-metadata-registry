package org.cardanofoundation.tokenmetadata.registry.job.model;

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
