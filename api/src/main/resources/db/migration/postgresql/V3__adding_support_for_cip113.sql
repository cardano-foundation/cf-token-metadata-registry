CREATE TABLE cip113_registry_node (
    -- policy_id stores the 'key' field from the CIP-113 registry node datum (first field in the linked list node)
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

-- Cip113RegistryNodeRepository.deleteBySlotGreaterThan(Long)
-- Query: WHERE slot > ?
-- Used by: Cip113EventListener.handleRollback(RollbackEvent)
CREATE INDEX idx_cip113_registry_node_slot ON cip113_registry_node (slot);
