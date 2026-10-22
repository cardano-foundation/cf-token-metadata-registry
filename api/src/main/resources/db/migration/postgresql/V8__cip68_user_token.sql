-- The CIP-68 fungible (333) and rich fungible (444) user tokens that have been seen on chain, so that the V2 API serves
-- the metadata of a 333 or 444 subject only when its token exists (ADR-022, ported from bloxbean/yaci-store#1294). The
-- metadata itself is in metadata_reference_nft. The table is filled from chain data: it is empty after this migration,
-- and a sync from scratch fills it.
CREATE TABLE IF NOT EXISTS cip68_user_token (
    unit     VARCHAR(120) PRIMARY KEY,
    slot     BIGINT       NOT NULL,
    tx_hash  VARCHAR(64)  NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_cip68_user_token_slot ON cip68_user_token (slot);
