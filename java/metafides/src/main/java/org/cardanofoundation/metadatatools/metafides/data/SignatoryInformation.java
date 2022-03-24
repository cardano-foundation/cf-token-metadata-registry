package org.cardanofoundation.metadatatools.metafides.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class SignatoryInformation {
    private String walletReceiveAddress;
    private String publicKey;
}
