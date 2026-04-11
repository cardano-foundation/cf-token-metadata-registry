-- CIP-26 off-chain token metadata.
--
-- All text-field bounds match the CIP-26 validator in cf-tokens-cip26:
-- name ≤ 50, ticker 2–9, url ≤ 250, description ≤ 500.
-- Anything exceeding these limits is rejected by the validator before insert.
-- Kept in sync with yaci-store/extensions/assets-ext (table: ft_offchain_metadata).
-- Reference: https://github.com/cardano-foundation/CIPs/blob/main/CIP-0026/README.md
create table "metadata" (
    -- subject = policyId (28 bytes) + optional assetName (0-32 bytes), hex-encoded.
    -- CIP-26 spec: minLength 56, maxLength 120.
    "subject" varchar(120) primary key,
    -- CIP-26 'policy' field: base16 CBOR-encoded phase-1 monetary script (a native
    -- script), NOT the 28-byte policyId hash (that lives in the first 56 hex chars
    -- of 'subject'). CIP-26 spec bounds: minLength 56, maxLength 120. DO NOT shrink
    -- to varchar(56) — many real registry entries (time-locked or multisig scripts)
    -- exceed 56 hex chars.
    "policy" varchar(120),
    -- CIP-26 name: max 50 chars.
    "name" varchar(50),
    -- CIP-26 ticker: 2-9 chars.
    "ticker" varchar(9),
    -- CIP-26 url: max 250 chars.
    "url" varchar(250),
    -- CIP-26 description: max 500 chars per spec.
    "description" varchar(500),
    -- CIP-26 decimals: spec range [0, 19] inclusive (well-known property 'decimals').
    "decimals" integer,
	"updated" timestamp,
	"updated_by" varchar(255),
    "properties" jsonb
);
--rollback drop table "metadata" cascade;

alter table "metadata" add column "textsearch" tsvector 
    generated always as
        (setweight(to_tsvector('english', coalesce("name", '')), 'A') ||
         setweight(to_tsvector('english', coalesce("ticker", '')), 'A') ||
         setweight(to_tsvector('english', coalesce("description", '')), 'B') ||
         setweight(to_tsvector('english', coalesce("url", '')), 'C') ||
         setweight(to_tsvector('english', coalesce("updated_by", '')), 'C')) stored;

-- CIP-26 fungible token logos.
create table "logo" (
    -- Matches "metadata"."subject" bounds (CIP-26 spec: 56-120 hex chars).
    "subject" varchar(120) primary key,
    -- base64-encoded image, up to ~87400 chars per CIP-26 — kept as text.
    "logo" text,
    constraint "fk_logo_metadata_subject" foreign key("subject") references "metadata"("subject")
);
--rollback drop table "logo" cascade;

create index "idx_metadata_defaultfields" on "metadata"("subject", "policy", "name", "ticker", "url", "description", "decimals", "updated" desc, "updated_by");
--rollback drop index "idx_metadata_defaultfields";

create index "idx_metadata_properties" on "metadata" using gin("properties");
--rollback drop index "idx_metadata_properties";

create table "wallet_scam_lookup" (
    "wallet_hash" varchar(64),
	"scam_incident_id" bigint,
    "domain" varchar(32),
    "reported" timestamp,
    primary key ("wallet_hash", "scam_incident_id")
);
--rollback drop table "wallet_scam_lookup" cascade;