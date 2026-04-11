package org.cardanofoundation.tokenmetadata.registry.entity;


import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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

    /** Policy id: exactly 28 bytes = 56 hex chars (Blake2b-224). Protocol-bounded. */
    @Id
    @Column(name = "policy_id", length = 56, nullable = false)
    private String policyId;

    /** Asset name: 0–32 bytes = 0–64 hex chars (Cardano ledger max). Protocol-bounded. */
    @Id
    @Column(name = "asset_name", length = 64, nullable = false)
    private String assetName;

    @Id
    private Long slot;

    /**
     * CIP-68 FT metadata 'name': variable UTF-8 string from the datum. 255 is lenient
     * but bounded; real tokens are typically ≤ 32 chars.
     */
    @Column(nullable = false, length = 255)
    private String name;

    /** CIP-68 FT 'description': arbitrary multi-sentence text; stored as TEXT. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /** CIP-68 FT 'ticker': short symbol; 32 is lenient (CIP-26 convention is 2–9). */
    @Column(length = 32)
    private String ticker;

    /** CIP-68 FT 'url': aligns with CIP-26 url cap of 250 chars. */
    @Column(length = 250)
    private String url;

    /** CIP-68 FT 'decimals': practically 0–255 (uint8). */
    private Long decimals;

    @Basic(fetch = FetchType.LAZY)
    @Column(columnDefinition = "TEXT")
    private String logo;

    /** CIP-68 schema version, typically 1. */
    @Column(nullable = false)
    private Long version;

    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String datum;

}
