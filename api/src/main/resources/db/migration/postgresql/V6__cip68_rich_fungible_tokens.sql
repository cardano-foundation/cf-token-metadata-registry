-- CIP-68 rich fungible tokens (label 444, user token prefix 001bc280) are served next to fungible tokens (label 333).
--
-- CIP-68 requires name and image for a 444 datum; description is optional (it is required only for 333). Their datums
-- are stored without a description, so the column becomes nullable. A 333 datum is still stored only with one.
ALTER TABLE metadata_reference_nft ALTER COLUMN description DROP NOT NULL;
