-- Tokens that break their CIP are indexed and reported, not dropped (ADR-019), as in yaci-store (its ADR 0006).
-- What is still dropped is recorded in invalid_token (ADR-020, yaci-store ADR 0007).
-- A full resync is needed for complete results: tokens dropped before exist nowhere, and rows written before have no
-- errors recorded (validation_errors NULL means "never checked", which the API tells apart from [] "no errors").

-- validation_errors holds the list of {cip, sub_standard, field_name, field_category, code, error}.
ALTER TABLE metadata ADD COLUMN IF NOT EXISTS validation_errors JSONB;
ALTER TABLE metadata_reference_nft ADD COLUMN IF NOT EXISTS validation_errors JSONB;

-- CIP-26: the limits of the CIP are reported as validation errors, so the columns hold what is in the registry.
-- textsearch is generated from these columns and PostgreSQL does not change the type of a column a generated column
-- depends on, so it is dropped and created again, unchanged. idx_metadata_defaultfields is a B-tree over text columns:
-- a long value would exceed the size of an index row and fail the insert, and the table is read by subject only, so it
-- goes. A decimals value is stored as written, so it needs the range of the BIGINT the entity already maps it to.
ALTER TABLE metadata DROP COLUMN IF EXISTS textsearch;
DROP INDEX IF EXISTS idx_metadata_defaultfields;
ALTER TABLE metadata ALTER COLUMN name TYPE TEXT;
ALTER TABLE metadata ALTER COLUMN ticker TYPE TEXT;
ALTER TABLE metadata ALTER COLUMN url TYPE TEXT;
ALTER TABLE metadata ALTER COLUMN decimals TYPE BIGINT;
ALTER TABLE metadata ADD COLUMN textsearch tsvector
    GENERATED ALWAYS AS
        (setweight(to_tsvector('english', coalesce(name, '')), 'A') ||
         setweight(to_tsvector('english', coalesce(ticker, '')), 'A') ||
         setweight(to_tsvector('english', coalesce(description, '')), 'B') ||
         setweight(to_tsvector('english', coalesce(url, '')), 'C') ||
         setweight(to_tsvector('english', coalesce(updated_by, '')), 'C')) STORED;

-- CIP-68: every 333 and 444 datum that can be decoded is stored, whatever it lacks, so name and version can be absent
-- (description already can, since V6). image and mediaType are stored as written (a 444 token's image is served as its
-- logo), and properties holds files and additional properties as JSON.
ALTER TABLE metadata_reference_nft ALTER COLUMN name DROP NOT NULL;
ALTER TABLE metadata_reference_nft ALTER COLUMN version DROP NOT NULL;
ALTER TABLE metadata_reference_nft ADD COLUMN IF NOT EXISTS image TEXT;
ALTER TABLE metadata_reference_nft ADD COLUMN IF NOT EXISTS media_type TEXT;
ALTER TABLE metadata_reference_nft ADD COLUMN IF NOT EXISTS properties JSONB;

-- What is dropped or unusable and has no token row to carry validation_errors: one row per item. The id is a SHA-256
-- of what identifies the item, so a replayed block or a re-run sync never duplicates a row.
CREATE TABLE IF NOT EXISTS invalid_token (
    id             VARCHAR(64)  PRIMARY KEY,
    standard       VARCHAR(16)  NOT NULL,
    combined_with  VARCHAR(16),
    reason_code    VARCHAR(64)  NOT NULL,
    reason         TEXT         NOT NULL,
    policy_id      VARCHAR(56),
    asset_name     VARCHAR(64),
    subject        VARCHAR(256),
    slot           BIGINT,
    tx_hash        VARCHAR(64),
    output_index   INTEGER,
    origin         VARCHAR(512),
    details        JSONB,
    first_seen_at  TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_invalid_token_slot ON invalid_token (slot);
CREATE INDEX IF NOT EXISTS idx_invalid_token_reason ON invalid_token (standard, reason_code);
CREATE INDEX IF NOT EXISTS idx_invalid_token_policy ON invalid_token (policy_id);
CREATE INDEX IF NOT EXISTS idx_invalid_token_origin ON invalid_token (standard, origin);
