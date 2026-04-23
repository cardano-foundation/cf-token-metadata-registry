package org.cardanofoundation.tokenmetadata.registry.entity;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class Cip113RegistryNodeId {

    private String key;
    private Long slot;
    private String txHash;

}
