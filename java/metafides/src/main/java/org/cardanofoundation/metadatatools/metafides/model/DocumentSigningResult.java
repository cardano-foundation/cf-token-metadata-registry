package org.cardanofoundation.metadatatools.metafides.model;

import lombok.*;

@Data
@NoArgsConstructor
public class DocumentSigningResult {
    private String documentHash;
    private String documentSignature;
    private String userDataSignature;
    private String transactionId;
}
