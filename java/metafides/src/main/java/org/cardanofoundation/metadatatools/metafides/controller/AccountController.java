package org.cardanofoundation.metadatatools.metafides.controller;

import org.cardanofoundation.metadatatools.metafides.model.AccountInformation;
import org.cardanofoundation.metadatatools.metafides.model.data.Account;
import org.cardanofoundation.metadatatools.metafides.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/account")
public class AccountController {
    @Autowired
    private AccountRepository accountRepository;

    @RequestMapping(value = "", method = RequestMethod.GET, produces = "application/json")
    public Flux<AccountInformation> getAccountInformation(@RequestAttribute("clientId") final String clientId) {
        return accountRepository.findByClientId(clientId).flatMap(
                account -> Flux.just(
                        AccountInformation.builder()
                                .walletReceiveAddress(account.getWalletAddress())
                                .publicKey(account.getPublicKey())
                                .build()
                )
        );
    }

    @RequestMapping(value = "/enroll", method = RequestMethod.POST, produces = "application/json")
    public Mono<AccountInformation> enroll(@RequestAttribute("clientId") final String clientId, @RequestBody final String publicKey, @RequestBody final String clientIdSignature) {
        final Account account = new Account();
        account.setClientId(clientId);
        account.setPublicKey(publicKey);
        account.setEnrolledDate(LocalDateTime.now());
        account.setWalletAddress("addr1qxhd9druzggfcls2wtpcqa0hr6vjprq6x3sfdgjftwgzfyltdeu6erfntvw3k8z2fgkvteua2zerd4tuanw7juxwlfgqm7w9w5");
        accountRepository.save(account);

        return Mono.just(AccountInformation.builder()
                .publicKey(account.getPublicKey())
                .walletReceiveAddress(account.getWalletAddress())
                .enrolledDate(account.getEnrolledDate())
                .build()
        );
    }
}
