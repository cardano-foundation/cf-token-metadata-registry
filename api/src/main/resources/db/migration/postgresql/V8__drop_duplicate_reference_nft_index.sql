-- idx_metadata_ref_nft_policy_asset_slot (V4) has the columns of the primary key, (policy_id, asset_name, slot): a
-- B-tree is scanned in either direction, so the primary key serves ORDER BY slot DESC as well, and the V4 index only
-- doubled the index writes of every reference NFT row (#155)
DROP INDEX IF EXISTS idx_metadata_ref_nft_policy_asset_slot;
