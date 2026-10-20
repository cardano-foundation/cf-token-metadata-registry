package org.cardanofoundation.tokenmetadata.registry.entity;


import jakarta.annotation.Nullable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "metadata_reference_nft")
@IdClass(MetadataReferenceNftId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetadataReferenceNft {

    @Id
    private String policyId;
    @Id
    private String assetName;
    @Id
    private Long slot;

    private String name;

    /** Required for a fungible token (label 333), optional for a rich fungible token (label 444). */
    @Nullable
    private String description;

    @Nullable
    private String ticker;

    @Nullable
    private String url;

    @Nullable
    private Long decimals;

    @Nullable
    private String logo;

    private Long version;

    private String datum;

}
