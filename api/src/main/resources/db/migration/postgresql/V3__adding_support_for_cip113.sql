CREATE TABLE cip113_registry_node (
    policy_id TEXT NOT NULL,
    slot BIGINT NOT NULL,
    tx_hash TEXT NOT NULL,
    transfer_logic_script TEXT,
    third_party_transfer_logic_script TEXT,
    global_state_policy_id TEXT,
    next_key TEXT NOT NULL,
    datum TEXT NOT NULL,
    PRIMARY KEY (policy_id, slot, tx_hash)
);

CREATE INDEX idx_cip113_registry_node_policy_slot ON cip113_registry_node (policy_id, slot DESC);

-- Cip113RegistryNodeRepository.deleteBySlotGreaterThan(Long)
-- Query: WHERE slot > ?
-- Used by: Cip113EventListener.handleRollback(RollbackEvent)
CREATE INDEX idx_cip113_registry_node_slot ON cip113_registry_node (slot);

-- MetadataReferenceNftRepository.findFirstByPolicyIdAndAssetNameOrderBySlotDesc(String, String)
-- Query: WHERE policy_id = ? AND asset_name = ? ORDER BY slot DESC LIMIT 1
-- Used by: Cip68FungibleTokenService on every V2 API subject lookup
CREATE INDEX idx_metadata_ref_nft_policy_asset_slot ON metadata_reference_nft (policy_id, asset_name, slot DESC);

-- MetadataReferenceNftRepository.deleteBySlotGreaterThan(Long)
-- Query: WHERE slot > ?
-- Used by: Cip68EventListener.handleRollback(RollbackEvent)
CREATE INDEX idx_metadata_ref_nft_slot ON metadata_reference_nft (slot);
