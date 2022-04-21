package org.cardanofoundation.metadatatools.metafides.controller;

import org.cardanofoundation.metadatatools.metafides.model.SignatoriesInformation;
import org.cardanofoundation.metadatatools.metafides.model.SignatoryInformation;
import org.cardanofoundation.metadatatools.metafides.model.data.Signatory;
import org.cardanofoundation.metadatatools.metafides.repository.SignatoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/signatory")
public class SignatoryController {
    @Autowired
    SignatoryRepository signatoryRepository;

    @RequestMapping(value = "/{signatoryId}", method = RequestMethod.GET, produces = "application/json")
    public Mono<SignatoryInformation> getSignatory(@RequestAttribute("clientId") final String clientId, @PathVariable final String signatoryId) {
        return Mono.just(new SignatoryInformation());
    }

    @RequestMapping(value = "", method = RequestMethod.GET, produces = "application/json")
    public Mono<SignatoriesInformation> getSignatories(@RequestAttribute("clientId") final String clientId) {
        return Mono.just(new SignatoriesInformation());
    }

    @RequestMapping(value = "", method = RequestMethod.POST, produces = "application/json")
    public Mono<SignatoryInformation> createSignatory(@RequestAttribute("clientId") final String clientId, final String signatoryId) {
        final Signatory signatory = new Signatory();
        signatory.setCreationDate(LocalDateTime.now());
        signatory.setClientId(clientId);
        signatory.setSequenceNumber(1);
        signatory.setPublicKey("012345");
        signatory.setWalletAddress("addr1xxx");
        return Mono.just(SignatoryInformation.builder()
                .publicKey(signatory.getPublicKey())
                .walletReceiveAddress(signatory.getWalletAddress())
                .build()
        );
    }
}
