-- CIP-113 programmable token registry nodes.
--
-- The datum is an Aiken `RegistryNode` sorted linked list entry. See CIP-143
-- (the parent spec) and cardano-foundation/cip113-programmable-tokens.
-- Kept in sync with yaci-store/extensions/assets-ext (same table name).
CREATE TABLE cip113_registry_node (
    -- 'key' field of the registry node datum. Three possible values:
    --   * empty string     — head sentinel of the sorted linked list
    --   * 56 hex chars     — a real 28-byte policy_id
    --   * 58–64 hex chars  — tail sentinel (conventionally 32 bytes of 0xFF
    --                        in the aiken-linked-list library)
    -- VARCHAR(64) is the tightest bound that fits all three. DO NOT shrink to 56.
    policy_id VARCHAR(64) NOT NULL,
    slot BIGINT NOT NULL,
    -- Cardano transaction hash: exactly 32 bytes = 64 hex chars. Protocol-bounded.
    tx_hash VARCHAR(64) NOT NULL,
    -- Aiken Credential inner hash (either VerificationKey or Script): 28 bytes = 56 hex.
    -- The constructor variant (VKey vs Script) is currently NOT preserved — if that
    -- distinction becomes important downstream, add a separate column.
    transfer_logic_script VARCHAR(56),
    third_party_transfer_logic_script VARCHAR(56),
    -- Currency symbol of the global-state NFT (28-byte policy_id). Protocol-bounded.
    global_state_policy_id VARCHAR(56),
    -- 'next' field — sorted linked list pointer. Same length range as 'policy_id' above
    -- (head is never a 'next'; real policy or tail sentinel 58–64 hex chars).
    next_key VARCHAR(64) NOT NULL,
    -- Full CBOR hex of the inline datum. Variable length.
    datum TEXT NOT NULL,
    PRIMARY KEY (policy_id, slot, tx_hash)
);

-- Cip113RegistryNodeRepository.deleteBySlotGreaterThan(Long)
-- Query: WHERE slot > ?
-- Used by: Cip113EventListener.handleRollback(RollbackEvent)
CREATE INDEX idx_cip113_registry_node_slot ON cip113_registry_node (slot);
