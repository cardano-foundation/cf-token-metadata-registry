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
public class SignatoriesInformation {
    private List<String> signatories;
    private int count;
}
