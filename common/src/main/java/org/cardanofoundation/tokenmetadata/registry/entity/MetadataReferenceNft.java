package org.cardanofoundation.tokenmetadata.registry.entity;


import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.*;
import org.cardanofoundation.tokenmetadata.registry.validation.ValidationError;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;

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

    /** Required by every label; a datum without one is stored with a validation error (ADR-019). */
    @Nullable
    private String name;

    /** Required for a fungible token (label 333), optional for a rich fungible token (label 444). */
    @Nullable
    private String description;

    @Nullable
    private String ticker;

    @Nullable
    private String url;

    /** As written, even outside the range CIP-68 allows (with a validation error); null when it is not an integer. */
    @Nullable
    private Long decimals;

    /** As written, even when it is not a URI with a scheme CIP-68 allows (with a validation error). */
    @Nullable
    private String logo;

    /** The NFT and RFT {@code image}, as written; a rich fungible token (444) serves it as its logo. */
    @Nullable
    private String image;

    @Column(name = "media_type")
    @Nullable
    private String mediaType;

    /** Null when the datum's version does not fit a 64-bit integer (with a validation error). */
    @Nullable
    private Long version;

    private String datum;

    /** The datum's files and additional properties as JSON: {@code {"files": [...], "additional_properties": {...}}}. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Nullable
    private Map<String, Object> properties;

    /**
     * How the datum itself departs from CIP-68, whatever the label of the token. What the label requires is checked when
     * the token is served, for the label asked for: the label is often guessed when the datum is indexed, and one
     * reference NFT can serve a 333 and a 444 token. The datum is stored and served anyway (ADR-019). Null on rows
     * written before validation existed.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_errors")
    @Nullable
    private List<ValidationError> validationErrors;

}
