-- CIP-68 on-chain reference NFT metadata.
--
-- On-chain columns (policy_id, asset_name) are protocol-bounded by the Cardano
-- multi-asset ledger rules. Datum-sourced string columns (name, ticker, url) use
-- lenient but explicit bounds — the datum itself is schema-flexible PlutusMap,
-- but realistic values fit within the limits below.
-- Kept in sync with yaci-store/extensions/assets-ext (same table name).
CREATE TABLE metadata_reference_nft (
    -- policy_id: exactly 28 bytes = 56 hex chars (Blake2b-224). Protocol-bounded.
    policy_id  VARCHAR(56)  NOT NULL,
    -- asset_name: 0–32 bytes = 0–64 hex chars (Cardano ledger max). Protocol-bounded.
    asset_name VARCHAR(64)  NOT NULL,
    slot       BIGINT       NOT NULL,
    -- CIP-68 FT 'name': variable UTF-8 string from the datum. 255 is lenient but bounded;
    -- real tokens are typically ≤ 32 chars.
    name       VARCHAR(255) NOT NULL,
    -- CIP-68 FT 'description': arbitrarily long multi-sentence text. Kept as TEXT.
    description TEXT        NOT NULL,
    -- CIP-68 FT 'ticker': short symbol (CIP-26 convention is 2–9); 32 is lenient.
    ticker     VARCHAR(32),
    -- CIP-68 FT 'url': aligns with CIP-26 url cap of 250 chars.
    url        VARCHAR(250),
    -- CIP-68 FT 'decimals': unsigned integer in the datum, no explicit CIP-68 spec cap.
    -- In practice 0–19 (aligned with CIP-26's well-known 'decimals' property).
    decimals   INTEGER,
    -- base64-encoded image (PNG/JPG/SVG). Variable-length, can be tens of KB.
    logo       TEXT,
    -- CIP-68 schema version, typically 1.
    version    BIGINT       NOT NULL,
    -- Full CBOR hex of the inline datum. Variable length, kept for reparsing/auditing.
    datum      TEXT         NOT NULL,
    PRIMARY KEY (policy_id, asset_name, slot)
)
--rollback drop table "metadata_reference_nft" cascade;
