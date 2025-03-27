package org.cardanofoundation.tokenmetadata.registry.entity.projections;

public interface MetadataReferenceNftProjection {

    String policyId();

    String assetName();

    Long slot();

    String name();

    String description();

    String ticker();

    String url();

    Long decimals();

    String logo();

    Long version();

    String datum();

}
