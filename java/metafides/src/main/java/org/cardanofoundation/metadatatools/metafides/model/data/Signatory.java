package org.cardanofoundation.metadatatools.metafides.model.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Signatory {
    @Id
    @Column("signatory_sequence_number")
    private Integer sequenceNumber;

    @Column("client_id")
    private String clientId;

    @Column("external_id")
    private String externalId;

    @Column("public_key")
    private String publicKey;

    @Column("wallet_address")
    private String walletAddress;

    @Column("created")
    private LocalDateTime creationDate;
}
