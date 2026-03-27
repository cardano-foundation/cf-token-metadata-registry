package org.cardanofoundation.tokenmetadata.registry.api.service.cip113;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.api.config.Cip113Configuration;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip113.Cip113DisplayMetadata;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip113.Cip113RegistryEntry;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip113.ProgrammableTokenCip113;
import org.cardanofoundation.tokenmetadata.registry.api.util.AssetType;
import org.cardanofoundation.tokenmetadata.registry.entity.Cip113RegistryNode;
import org.cardanofoundation.tokenmetadata.registry.entity.MetadataReferenceNft;
import org.cardanofoundation.tokenmetadata.registry.repository.Cip113RegistryNodeRepository;
import org.cardanofoundation.tokenmetadata.registry.repository.MetadataReferenceNftRepository;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class Cip113RegistryService {

    private final Cip113RegistryNodeRepository cip113RegistryNodeRepository;
    private final MetadataReferenceNftRepository metadataReferenceNftRepository;
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

    /**
     * Single policy lookup, enriched with CIP-68 display metadata.
     */
    public Optional<Cip113RegistryEntry> findRegistryEntry(String policyId) {
        if (!cip113Configuration.isEnabled()) {
            return Optional.empty();
        }
        return cip113RegistryNodeRepository.findFirstByPolicyIdOrderBySlotDesc(policyId)
                .map(this::enrichSingleEntry);
    }

    /**
     * Batch lookup by policy IDs, enriched with CIP-68 display metadata.
     */
    public List<Cip113RegistryEntry> findRegistryEntries(Collection<String> policyIds) {
        if (!cip113Configuration.isEnabled() || policyIds.isEmpty()) {
            return List.of();
        }
        List<Cip113RegistryNode> nodes = cip113RegistryNodeRepository.findLatestByPolicyIds(policyIds);
        return enrichWithDisplay(nodes);
    }

    private List<Cip113RegistryEntry> enrichWithDisplay(List<Cip113RegistryNode> nodes) {
        List<String> policyIds = nodes.stream().map(Cip113RegistryNode::getPolicyId).toList();
        Map<String, Cip113DisplayMetadata> displayMap = fetchDisplayMetadata(policyIds);
        return nodes.stream()
                .map(node -> toRegistryEntry(node, displayMap.get(node.getPolicyId())))
                .toList();
    }

    private Cip113RegistryEntry enrichSingleEntry(Cip113RegistryNode node) {
        Map<String, Cip113DisplayMetadata> displayMap = fetchDisplayMetadata(List.of(node.getPolicyId()));
        return toRegistryEntry(node, displayMap.get(node.getPolicyId()));
    }

    private Map<String, Cip113DisplayMetadata> fetchDisplayMetadata(List<String> policyIds) {
        if (policyIds.isEmpty()) {
            return Map.of();
        }
        return metadataReferenceNftRepository.findLatestByPolicyIds(policyIds)
                .stream()
                .collect(Collectors.toMap(
                        MetadataReferenceNft::getPolicyId,
                        Cip113DisplayMetadata::from,
                        (first, second) -> first
                ));
    }

    private static Cip113RegistryEntry toRegistryEntry(Cip113RegistryNode node, @Nullable Cip113DisplayMetadata display) {
        return new Cip113RegistryEntry(
                node.getPolicyId(),
                node.getTransferLogicScript(),
                node.getThirdPartyTransferLogicScript(),
                node.getGlobalStatePolicyId(),
                display
        );
    }

    private static ProgrammableTokenCip113 toDto(Cip113RegistryNode entity) {
        return new ProgrammableTokenCip113(
                entity.getTransferLogicScript(),
                entity.getThirdPartyTransferLogicScript(),
                entity.getGlobalStatePolicyId()
        );
    }

}
