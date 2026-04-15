package org.cardanofoundation.tokenmetadata.registry.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.*;
import jakarta.annotation.Nullable;

@Entity
@Table(name = "cip113_registry_node")
@IdClass(Cip113RegistryNodeId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cip113RegistryNode {

    /**
     * The {@code key} field of the CIP-113 registry node datum. Three possible values:
     * <ul>
     *   <li>empty string — head sentinel of the sorted linked list,</li>
     *   <li>56 hex chars — a real 28-byte policy_id,</li>
     *   <li>up to 64 hex chars — tail sentinel. The aiken-linked-list implementation in use on
     *       preprod materializes a 30-byte (60-hex) sentinel of {@code 0xFF}; the upper bound of
     *       32 bytes (64 hex) is kept for safety since the exact length is a library-level
     *       convention, not a CIP-113 protocol guarantee.</li>
     * </ul>
     * {@code VARCHAR(64)} is the tightest bound that fits all three — DO NOT shrink to 56.
     */
    @Id
    @Column(name = "key", length = 64, nullable = false)
    private String key;

    @Id
    private Long slot;

    /** Cardano transaction hash: exactly 32 bytes = 64 hex chars. Protocol-bounded. */
    @Id
    @Column(name = "tx_hash", length = 64, nullable = false)
    private String txHash;

    /** Aiken {@code Credential} inner hash (28-byte vkey or script hash, 56 hex chars). */
    @Nullable
    @Column(name = "transfer_logic_script", length = 56)
    private String transferLogicScript;

    /** Aiken {@code Credential} inner hash (28-byte vkey or script hash, 56 hex chars). */
    @Nullable
    @Column(name = "third_party_transfer_logic_script", length = 56)
    private String thirdPartyTransferLogicScript;

    /** Currency symbol of the global-state NFT (28-byte policy_id, 56 hex chars). */
    @Nullable
    @Column(name = "global_state_policy_id", length = 56)
    private String globalStatePolicyId;

    /**
     * The {@code next} field of the CIP-113 registry node datum — the pointer in the sorted
     * linked list. Same length range as {@link #key}: either a real 56-hex policy_id or
     * a pointer to the tail sentinel (60 hex chars on preprod; up to 64 hex allowed for safety).
     * {@code VARCHAR(64)} is the tightest bound — DO NOT shrink to 56.
     */
    @Column(name = "next", length = 64, nullable = false)
    private String next;

    @Column(columnDefinition = "TEXT")
    private String datum;

}
