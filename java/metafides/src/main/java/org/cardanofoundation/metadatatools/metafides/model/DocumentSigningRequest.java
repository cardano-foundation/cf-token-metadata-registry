package org.cardanofoundation.metadatatools.metafides.model;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
public class DocumentSigningRequest {
    private String documentLink;
    private List<String> signatories;
}
