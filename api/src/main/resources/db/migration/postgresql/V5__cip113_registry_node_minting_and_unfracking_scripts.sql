-- CIP-113 registry nodes: minting and unfracking logic scripts.
--
-- The RegistryNode datum deployed on mainnet and preprod (cip113-programmable-tokens, deployment
-- schemaVersion 3) has 7 fields:
--   [key, next, minting_logic_script, transfer_logic_script, third_party_logic_script,
--    unfracking_logic_script, global_state_cs]
-- V3 stored the 5-field layout without the two new credentials. Both are Aiken Credential inner
-- hashes (VerificationKey or Script): 28 bytes = 56 hex, NULL when absent. A NULL
-- unfracking_logic_script means unfracking is forbidden for the policy (empty_vkey on-chain).
ALTER TABLE cip113_registry_node ADD COLUMN minting_logic_script VARCHAR(56);
ALTER TABLE cip113_registry_node ADD COLUMN unfracking_logic_script VARCHAR(56);
