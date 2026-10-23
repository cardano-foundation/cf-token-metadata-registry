package org.cardanofoundation.tokenmetadata.registry.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import jakarta.annotation.Nullable;
import org.cardanofoundation.tokenmetadata.registry.model.Mapping;
import org.cardanofoundation.tokenmetadata.registry.validation.ValidationError;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "metadata")
@Getter
@Setter
public class TokenMetadata {

    @Id
    private String subject;

    private String policy;

    private String name;

    private String ticker;

    private String url;

    private String description;

    private Long decimals;

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime updated;

    private String updatedBy;

    @JdbcTypeCode(SqlTypes.JSON)
    private Mapping properties;

    /**
     * How the entry departs from CIP-26. The entry is stored and served anyway (ADR-019). Null on rows written before
     * validation existed (never checked), which is not the same as an empty list (checked, no errors).
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_errors")
    @Nullable
    private List<ValidationError> validationErrors;


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
