package org.cardanofoundation.metadatatools.metafides;

import org.cardanofoundation.metadatatools.metafides.data.SignatoriesInformation;
import org.cardanofoundation.metadatatools.metafides.data.SignatoryInformation;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/signatory")
public class SignatoryController {
    @RequestMapping(value = "/{signatoryId}", method = RequestMethod.GET, produces = "application/json")
    public SignatoryInformation getSignatory(final String signatoryId) {
        return new SignatoryInformation();
    }

    @RequestMapping(value = "", method = RequestMethod.GET, produces = "application/json")
    public SignatoriesInformation getSignatories() {
        return new SignatoriesInformation();
    }

    @RequestMapping(value = "", method = RequestMethod.PUT, produces = "application/json")
    public SignatoryInformation createSignatory(final String signatoryId, final String signatoryIdSignature) {
        return new SignatoryInformation();
    }
}
