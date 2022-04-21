package org.cardanofoundation.metadatatools.metafides.model;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SignatoryInformation {
    private String walletReceiveAddress;
    private String publicKey;
}
