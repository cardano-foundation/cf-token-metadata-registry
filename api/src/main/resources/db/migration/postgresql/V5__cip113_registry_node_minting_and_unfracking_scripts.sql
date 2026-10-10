-- CIP-113 registry nodes: minting and unfracking logic scripts.
--
-- RegistryNode datum (cip113-programmable-tokens, lib/registry_node.ak):
--   [key, next, minting_logic_script, transfer_logic_script, third_party_logic_script,
--    unfracking_logic_script, global_state_cs]
-- Both columns hold an Aiken Credential inner hash (VerificationKey or Script): 28 bytes = 56 hex,
-- NULL when absent. A NULL unfracking_logic_script means unfracking is forbidden for the policy
-- (empty_vkey on-chain).
ALTER TABLE cip113_registry_node ADD COLUMN minting_logic_script VARCHAR(56);
ALTER TABLE cip113_registry_node ADD COLUMN unfracking_logic_script VARCHAR(56);
