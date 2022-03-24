package org.cardanofoundation.metadatatools.core.model;

import lombok.*;

@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.PUBLIC)
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class AttestationSignature {
    private String signature;
    private String publicKey;
}
