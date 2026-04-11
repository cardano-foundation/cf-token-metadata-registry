package org.cardanofoundation.tokenmetadata.registry.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.cardanofoundation.tokenmetadata.registry.model.Mapping;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "metadata")
@Getter
@Setter
public class TokenMetadata {

    /** Subject = policyId (28 bytes) + optional assetName (0-32 bytes), hex. CIP-26 spec 56-120. */
    @Id
    @Column(length = 120)
    private String subject;

    /**
     * CIP-26 {@code policy} field: base16 CBOR-encoded phase-1 monetary script.
     * CIP-26 spec bounds: minLength 56, maxLength 120. DO NOT shrink to VARCHAR(56) —
     * many real registry entries use time-locked or multisig scripts exceeding 56 hex chars.
     */
    @Column(length = 120)
    private String policy;

    /** CIP-26 name: max 50 chars (enforced by cf-tokens-cip26 validator). */
    @Column(length = 50)
    private String name;

    /** CIP-26 ticker: 2-9 chars (enforced by cf-tokens-cip26 validator). */
    @Column(length = 9)
    private String ticker;

    /** CIP-26 url: max 250 chars (enforced by cf-tokens-cip26 validator). */
    @Column(length = 250)
    private String url;

    /** CIP-26 description: max 500 chars per spec. */
    @Column(length = 500)
    private String description;

    /** CIP-26 decimals: spec range [0, 19] inclusive (well-known property 'decimals'). */
    private Long decimals;

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime updated;

    private String updatedBy;

    @JdbcTypeCode(SqlTypes.JSON)
    private Mapping properties;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TokenMetadata that = (TokenMetadata) o;
        return Objects.equals(subject, that.subject);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subject);
    }
}
