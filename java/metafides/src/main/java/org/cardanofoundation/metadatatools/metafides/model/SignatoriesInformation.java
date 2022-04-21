package org.cardanofoundation.metadatatools.metafides.model;

import lombok.*;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SignatoriesInformation {
    private List<SignatoryInformation> signatories;
}
