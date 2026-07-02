package org.cardanofoundation.tokenmetadata.registry.api.service;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.utxo.domain.AddressUtxoEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip68.FungibleTokenMetadata;
import org.cardanofoundation.tokenmetadata.registry.api.util.AssetType;
import org.cardanofoundation.tokenmetadata.registry.entity.MetadataReferenceNft;
import org.cardanofoundation.tokenmetadata.registry.repository.MetadataReferenceNftRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class Cip68EventListener {

    private final Cip68FungibleTokenService cip68FungibleTokenService;
    private final Cip68FTDatumParser cip68DatumParser;
    private final MetadataReferenceNftRepository metadataReferenceNftRepository;

    @EventListener
    @Transactional
    public void handleRollback(RollbackEvent rollbackEvent) {
        long rollbackSlot = rollbackEvent.getRollbackTo().getSlot();
        int count = metadataReferenceNftRepository.deleteBySlotGreaterThan(rollbackSlot);
        log.info("CIP-68 rollback to slot {}: deleted {} reference NFT records", rollbackSlot, count);
    }

    @EventListener
    public void processTransaction(AddressUtxoEvent addressUtxoEvent) {
        Long slot = addressUtxoEvent.getMetadata().getSlot();
        List<MetadataReferenceNft> entities = addressUtxoEvent.getTxInputOutputs()
                .stream()
                .flatMap(txInputOutput -> txInputOutput.getOutputs().stream())
                .flatMap(this::findReferenceNft)
                .flatMap(this::parseDatum)
                .filter(this::isValidFTMetadata)
                .map(referenceNftUtxoData -> buildMetadataReferenceNft(
                        referenceNftUtxoData.fungibleTokenMetadata(),
                        referenceNftUtxoData.referenceNft(),
                        referenceNftUtxoData.datum(),
                        slot))
                .toList();

        if (!entities.isEmpty()) {
            metadataReferenceNftRepository.saveAll(entities);
        }
    }

    private MetadataReferenceNft buildMetadataReferenceNft(FungibleTokenMetadata metadata, AssetType assetType, String datum, Long slot) {
        return MetadataReferenceNft.builder()
                .policyId(assetType.policyId())
                .assetName(assetType.assetName())
                .slot(slot)
                .name(metadata.name())
                .description(metadata.description())
                .ticker(metadata.ticker())
                .url(metadata.url())
                .decimals(metadata.decimals())
                .logo(metadata.logo())
                .version(metadata.version())
                .datum(datum)
                .build();
    }

    private boolean isValidFTMetadata(ReferenceNftUtxoData referenceNftUtxoData) {
        return cip68FungibleTokenService.isValidFTMetadata(referenceNftUtxoData.fungibleTokenMetadata());
    }

    private Stream<ReferenceNftUtxoData> parseDatum(ReferenceNftUtxo referenceNftUtxo) {
        return cip68DatumParser.parse(referenceNftUtxo.utxo().getInlineDatum())
                .stream()
                .map(fungibleTokenMetadata -> new ReferenceNftUtxoData(AssetType.fromUnit(referenceNftUtxo.amt().getUnit()),
                        fungibleTokenMetadata,
                        referenceNftUtxo.utxo().getInlineDatum()));
    }

    private Stream<ReferenceNftUtxo> findReferenceNft(AddressUtxo utxo) {
        return cip68FungibleTokenService.extractReferenceNft(utxo).map(amt -> new ReferenceNftUtxo(amt, utxo)).stream();
    }

    private record ReferenceNftUtxo(Amt amt, AddressUtxo utxo) { }

    private record ReferenceNftUtxoData(AssetType referenceNft,
                                        FungibleTokenMetadata fungibleTokenMetadata,
                                        String datum) { }

}
