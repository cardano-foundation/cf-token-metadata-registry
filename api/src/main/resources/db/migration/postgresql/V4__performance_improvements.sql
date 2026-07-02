-- MetadataReferenceNftRepository.findFirstByPolicyIdAndAssetNameOrderBySlotDesc(String, String)
-- Query: WHERE policy_id = ? AND asset_name = ? ORDER BY slot DESC LIMIT 1
-- Used by: Cip68FungibleTokenService on every V2 API subject lookup
CREATE INDEX idx_metadata_ref_nft_policy_asset_slot ON metadata_reference_nft (policy_id, asset_name, slot DESC);

-- MetadataReferenceNftRepository.deleteBySlotGreaterThan(Long)
-- Query: WHERE slot > ?
-- Used by: Cip68EventListener.handleRollback(RollbackEvent)
CREATE INDEX idx_metadata_ref_nft_slot ON metadata_reference_nft (slot);
