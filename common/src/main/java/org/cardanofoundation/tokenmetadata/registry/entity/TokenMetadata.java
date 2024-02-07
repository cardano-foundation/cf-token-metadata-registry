package org.cardanofoundation.tokenmetadata.registry.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.cardanofoundation.tokenmetadata.registry.model.Mapping;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

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

}
