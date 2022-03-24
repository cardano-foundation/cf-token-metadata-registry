package org.cardanofoundation.metadatatools.metafides.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AccountInformation {
    private String walletReceiveAddress;
    private int currentAdaBalance;
}
