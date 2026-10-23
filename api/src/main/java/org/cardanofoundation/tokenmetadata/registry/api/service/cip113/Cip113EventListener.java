package org.cardanofoundation.tokenmetadata.registry.api.service.cip113;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.utxo.domain.AddressUtxoEvent;
import com.bloxbean.cardano.yaci.store.utxo.domain.TxInputOutput;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.api.config.Cip113Configuration;
import org.cardanofoundation.tokenmetadata.registry.api.event.Cip113RegistryChangedEvent;
import org.cardanofoundation.tokenmetadata.registry.api.util.AssetType;
import org.cardanofoundation.tokenmetadata.registry.entity.Cip113RegistryNode;
import org.cardanofoundation.tokenmetadata.registry.invalidtoken.InvalidTokenReason;
import org.cardanofoundation.tokenmetadata.registry.invalidtoken.InvalidTokenRecorder;
import org.cardanofoundation.tokenmetadata.registry.invalidtoken.InvalidTokenReport;
import org.cardanofoundation.tokenmetadata.registry.repository.Cip113RegistryNodeRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class Cip113EventListener {

    private static final int DATUM_PREFIX_CHARS = 256;

    private final Cip113Configuration cip113Configuration;
    private final Cip113RegistryNodeParser registryNodeParser;
    private final Cip113RegistryNodeRepository cip113RegistryNodeRepository;
    private final Cip113RegistryService cip113RegistryService;
    private final InvalidTokenRecorder invalidTokenRecorder;
    private final ApplicationEventPublisher eventPublisher;

    @EventListener
    @Transactional
    public void handleRollback(RollbackEvent rollbackEvent) {
        long rollbackSlot = rollbackEvent.getRollbackTo().getSlot();
        int count = cip113RegistryNodeRepository.deleteBySlotGreaterThan(rollbackSlot);
        log.info("CIP-113 rollback to slot {}: deleted {} registry node records", rollbackSlot, count);
        if (count > 0) {
            eventPublisher.publishEvent(Cip113RegistryChangedEvent.rolledBack(count));
        }
    }

    @Transactional
    @EventListener
    public void processTransaction(AddressUtxoEvent addressUtxoEvent) {
        if (!cip113Configuration.isEnabled()) {
            return;
        }

        Long slot = addressUtxoEvent.getMetadata().getSlot();

        List<Cip113RegistryNode> entities = new ArrayList<>();

        for (TxInputOutput txInputOutput : addressUtxoEvent.getTxInputOutputs()) {
            for (AddressUtxo utxo : txInputOutput.getOutputs()) {
                if (cip113RegistryService.containsRegistryNode(utxo)) {
                    parseEntity(utxo, slot).ifPresent(entities::add);
                }
            }
        }

        if (!entities.isEmpty()) {
            cip113RegistryNodeRepository.saveAll(entities);
            entities.forEach(entity -> log.info("Indexed CIP-113 registry node: key={}, slot={}, txHash={}",
                    entity.getKey(), entity.getSlot(), entity.getTxHash()));
            // a newly registered policy may have no CIP-26 or CIP-68 metadata yet (ADR-020)
            eventPublisher.publishEvent(Cip113RegistryChangedEvent.indexed(entities.size()));
        }
    }

    /**
     * The row for a registry node output. A node that cannot be parsed (the parser logs why) is recorded in
     * {@code invalid_token} as {@code registry_node_unparseable} (ADR-020), with the policy of its registry NFT.
     */
    private Optional<Cip113RegistryNode> parseEntity(AddressUtxo utxo, Long slot) {
        Cip113RegistryNodeParser.ParseResult result = registryNodeParser.parseDetailed(utxo.getInlineDatum());
        if (result.node() == null) {
            recordUnparseable(utxo, slot, result.rejection());
            return Optional.empty();
        }
        return Optional.of(result.node())
                .map(parsedRegistryNode -> Cip113RegistryNode.builder()
                        .key(parsedRegistryNode.key())
                        .slot(slot)
                        .txHash(utxo.getTxHash())
                        .mintingLogicScript(parsedRegistryNode.mintingLogicScript())
                        .transferLogicScript(parsedRegistryNode.transferLogicScript())
                        .thirdPartyTransferLogicScript(parsedRegistryNode.thirdPartyTransferLogicScript())
                        .unfrackingLogicScript(parsedRegistryNode.unfrackingLogicScript())
                        .globalStatePolicyId(parsedRegistryNode.globalStatePolicyId())
                        .next(parsedRegistryNode.next())
                        .datum(utxo.getInlineDatum())
                        .build());
    }

    private void recordUnparseable(AddressUtxo utxo, Long slot, String rejection) {
        Optional<AssetType> registryNft = cip113RegistryService.findRegistryNft(utxo);
        String datum = utxo.getInlineDatum();
        invalidTokenRecorder.report(InvalidTokenReport.builder()
                .standard(InvalidTokenReason.CIP_113)
                .reasonCode(InvalidTokenReason.REGISTRY_NODE_UNPARSEABLE)
                .reason(rejection)
                .policyId(registryNft.map(AssetType::policyId).orElse(null))
                .assetName(registryNft.map(AssetType::assetName).orElse(null))
                .slot(slot)
                .txHash(utxo.getTxHash())
                .outputIndex(utxo.getOutputIndex())
                .details(Map.of("datum_prefix", datum == null ? "" : datum.substring(0, Math.min(datum.length(), DATUM_PREFIX_CHARS))))
                .build());
    }

}
