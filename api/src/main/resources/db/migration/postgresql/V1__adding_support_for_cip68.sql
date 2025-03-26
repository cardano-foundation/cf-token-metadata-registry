CREATE TABLE metadata_reference_nft (
    policy_id TEXT,
    asset_name TEXT,
    slot BIGINT,
    name TEXT NOT NULL,
    description TEXT NOT NULL,
    ticker TEXT,
    url TEXT,
    decimals BIGINT,
    logo TEXT,
    version BIGINT NOT NULL,
    datum TEXT NOT NULL,
    PRIMARY KEY (policy_id, asset_name, slot)
)
--rollback drop table "metadata_reference_nft" cascade;
