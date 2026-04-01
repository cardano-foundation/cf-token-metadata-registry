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

@Service
@RequiredArgsConstructor
@Slf4j
public class Cip113RegistryService {

    private final Cip113RegistryNodeRepository cip113RegistryNodeRepository;
    private final Cip113Configuration cip113Configuration;

    /**
     * Look up the latest CIP-113 registry node for a given policy ID.
     */
    public Optional<ProgrammableTokenCip113> findByPolicyId(String policyId) {
        if (!cip113Configuration.isEnabled()) {
            return Optional.empty();
        }

        return cip113RegistryNodeRepository.findFirstByPolicyIdOrderBySlotDesc(policyId)
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

        return cip113RegistryNodeRepository.findLatestByPolicyIds(policyIds)
                .stream()
                .collect(Collectors.toMap(
                        Cip113RegistryNode::getPolicyId,
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
