package org.cardanofoundation.metadatatools.metafides.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class DocumentSigningResult {
    private String documentHash;
    private String documentSignature;
    private String userDataSignature;
    private String transactionId;
}
