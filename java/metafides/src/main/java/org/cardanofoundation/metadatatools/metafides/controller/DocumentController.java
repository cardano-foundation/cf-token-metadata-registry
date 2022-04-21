package org.cardanofoundation.metadatatools.metafides.controller;

import org.cardanofoundation.metadatatools.metafides.model.DocumentSigningRequest;
import org.cardanofoundation.metadatatools.metafides.model.DocumentSigningResult;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@RestController
@RequestMapping("/document")
public class DocumentController {
    @RequestMapping(value = "/signlink", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public DocumentSigningResult signDocument(@RequestAttribute("clientId") final String clientId, final DocumentSigningRequest documentSigningRequest) {
        return new DocumentSigningResult();
    }

    @RequestMapping(value = "/sign", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentSigningResult signDocument(@RequestAttribute("clientId") final String clientId, @RequestParam("file") MultipartFile file, @RequestParam("signatories") List<String> signatories) {
        return new DocumentSigningResult();
    }

    @RequestMapping(value = "/{documenthash}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public DocumentSigningResult getDocumentSigningInformation(@RequestAttribute("clientId") final String clientId, @PathVariable("documenthash") final String documentHash) {
        return new DocumentSigningResult();
    }

    @RequestMapping(value = "/{documenthash}/verify/{signature}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public DocumentSigningResult getDocumentSigningInformation(@RequestAttribute("clientId") final String clientId, @PathVariable("documenthash") final String documentHash, @PathVariable("signature") final String signature) {
        return new DocumentSigningResult();
    }
    /*
    /document (PUT): sign and upload documents
  - link to the document
  - user defined json to attach as offchain metadata
  - The signature of the document (based on PrivKey(ZK))
            - The signature of the json (based on PrivKey(ZK))
            - The signatory IDs this document shall be signed by
    Metafides creates the on-chain metadata document from it (doc link, hash, signatures w pubkeys)
    Metafides generates the transaction, signing it with corresponding PK and submits it to the chain
    Metafides responses with the transaction id and the hash of the document

/document/{documenthash} (GET): Verify if a document has been signed and retrieve some metadata about it
    Zeus calls docment verify endpoint (/document/{document_hash}/verfiy)
            - Checks if there is a transaction on chain of label xy that contains the
    - Returns errors depending on verification failure or likewise

/document/{documenthash}/signature/{signatory} (GET): get the signature for the document associated with the given signatory id
/document/{documenthash}/signature/{pubkeyhash} (GET): get the signature for the document associated with the given public key hash
*/
}
