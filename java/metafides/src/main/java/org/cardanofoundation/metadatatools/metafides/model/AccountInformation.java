package org.cardanofoundation.metadatatools.metafides.model;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountInformation {
    private String walletReceiveAddress;
    private String publicKey;
    private int totalLovelace;
    private LocalDateTime enrolledDate;
}
