package org.cardanofoundation.metadatatools.metafides;

import org.cardanofoundation.metadatatools.metafides.data.AccountInformation;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/account")
public class AccountController {
    @RequestMapping(value = "/{accountid}", method = RequestMethod.GET, produces = "application/json")
    public AccountInformation getAccountInformation(@RequestHeader("Authorization") String authorization, @RequestParam("accountid") String accountId) {
        return new AccountInformation();
    }

    @RequestMapping(value = "/enroll", method = RequestMethod.POST, produces = "application/json")
    public AccountInformation enroll(@RequestHeader("Authorization") String authorization, final String publicKey) {
        return new AccountInformation();
    }
}
