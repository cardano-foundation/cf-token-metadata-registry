package org.cardanofoundation.tokenmetadata.registry.entity;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class MetadataReferenceNftId {

    private String policyId;
    private String assetName;
    private Long slot;

}
