package org.cardanofoundation.metadatatools.metafides;

import org.cardanofoundation.metadatatools.metafides.data.DocumentSigningRequest;
import org.cardanofoundation.metadatatools.metafides.data.DocumentSigningResult;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/document")
public class DocumentController {
    @RequestMapping(value = "", method = RequestMethod.PUT, produces = "application/json")
    public DocumentSigningResult signDocument(final DocumentSigningRequest documentSigningRequest) {
        return new DocumentSigningResult();
    }

    @RequestMapping(value = "/{documenthash}", method = RequestMethod.GET, produces = "application/json")
    public DocumentSigningResult getDocumentSigningInformation(@PathVariable("documenthash") final String documentHash) {
        return new DocumentSigningResult();
    }

    @RequestMapping(value = "/{documenthash}/verify/{signature}", method = RequestMethod.GET, produces = "application/json")
    public DocumentSigningResult getDocumentSigningInformation(@PathVariable("documenthash") final String documentHash, @PathVariable("signature") final String signature) {
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
