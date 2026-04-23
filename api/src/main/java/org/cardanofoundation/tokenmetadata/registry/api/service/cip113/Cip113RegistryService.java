package org.cardanofoundation.tokenmetadata.registry.api.service.cip113;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.api.config.Cip113Configuration;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip113.ProgrammableTokenCip113;
import org.cardanofoundation.tokenmetadata.registry.api.util.AssetType;
import org.cardanofoundation.tokenmetadata.registry.entity.Cip113RegistryNode;
import org.cardanofoundation.tokenmetadata.registry.repository.Cip113RegistryNodeRepository;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for looking up CIP-113 programmable-token registry data by policy ID.
 *
 * <p><b>Naming note:</b> this service is deliberately named in policy-ID terms even though the
 * underlying column is {@code key} (see {@link Cip113RegistryNode#getKey()} — the registry's
 * {@code key} is a policy ID for real rows, a linked-list marker for sentinel rows). The
 * service contract with V2 API callers is <i>"give me the programmable-token extension for
 * this token's policy ID"</i>, so its public methods speak policy IDs and translate to a
 * {@code WHERE key = ?} lookup internally. Sentinels are never matched because no real token
 * has an empty or 32-byte-of-{@code 0xFF} policy ID.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class Cip113RegistryService {

    private final Cip113RegistryNodeRepository cip113RegistryNodeRepository;
    private final Cip113Configuration cip113Configuration;

    /**
     * Look up the latest CIP-113 registry node for a given policy ID.
     * <p>Internally this queries {@code WHERE key = policyId ORDER BY slot DESC LIMIT 1} —
     * real registered tokens have {@code key} equal to their 56-hex policy ID, so the lookup
     * succeeds for them and misses (correctly) for unregistered tokens and for sentinel rows.
     */
    public Optional<ProgrammableTokenCip113> findByPolicyId(String policyId) {
        if (!cip113Configuration.isEnabled()) {
            return Optional.empty();
        }

        return cip113RegistryNodeRepository.findFirstByKeyOrderBySlotDesc(policyId)
                .map(Cip113RegistryService::toDto);
    }

    /**
     * Batch lookup of CIP-113 registry nodes for multiple policy IDs.
     * Returns a map of policyId → ProgrammableTokenCip113 for policies that are registered.
     */
    public Map<String, ProgrammableTokenCip113> findByPolicyIds(Collection<String> policyIds) {
        if (!cip113Configuration.isEnabled() || policyIds.isEmpty()) {
            return Map.of();
        }

        return cip113RegistryNodeRepository.findLatestByKeys(policyIds)
                .stream()
                .collect(Collectors.toMap(
                        Cip113RegistryNode::getKey,
                        Cip113RegistryService::toDto
                ));
    }

    /**
     * Check if a UTxO contains a CIP-113 registry node NFT.
     * Used by CustomUtxoStorage to decide which UTxOs to persist.
     */
    public boolean containsRegistryNode(AddressUtxo utxo) {
        if (!cip113Configuration.isEnabled()) {
            return false;
        }

        return utxo.getAmounts().stream()
                .anyMatch(amt -> amt.getQuantity().equals(BigInteger.ONE)
                        && cip113Configuration.isMonitoredPolicyId(AssetType.fromUnit(amt.getUnit()).policyId()));
    }

    private static ProgrammableTokenCip113 toDto(Cip113RegistryNode entity) {
        String transferLogic = entity.getTransferLogicScript();
        String thirdParty = entity.getThirdPartyTransferLogicScript();
        String globalState = entity.getGlobalStatePolicyId();

        return new ProgrammableTokenCip113(
                (transferLogic == null || transferLogic.isEmpty()) ? null : transferLogic,
                (thirdParty == null || thirdParty.isEmpty()) ? null : thirdParty,
                (globalState == null || globalState.isEmpty()) ? null : globalState
        );
    }

}
