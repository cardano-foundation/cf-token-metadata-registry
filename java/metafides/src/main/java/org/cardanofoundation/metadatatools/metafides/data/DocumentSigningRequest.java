package org.cardanofoundation.metadatatools.metafides.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class DocumentSigningRequest {
    private String documentLink;
    private String userData;
    private String documentSignature;
    private String userDataSignature;
    private List<String> signatories;
}
