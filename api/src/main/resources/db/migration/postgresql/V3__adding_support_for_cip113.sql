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
