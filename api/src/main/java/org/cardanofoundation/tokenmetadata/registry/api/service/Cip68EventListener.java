package org.cardanofoundation.tokenmetadata.registry.api.service;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.utxo.domain.AddressUtxoEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.api.model.Pair;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip68.FungibleTokenMetadata;
import org.cardanofoundation.tokenmetadata.registry.api.util.AssetType;
import org.cardanofoundation.tokenmetadata.registry.entity.MetadataReferenceNft;
import org.cardanofoundation.tokenmetadata.registry.repository.MetadataReferenceNftRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class Cip68EventListener {

    private record ReferenceNftUtxoData(AssetType referenceNft, FungibleTokenMetadata fungibleTokenMetadata,
                                        String datum) {

    }

    private final Cip68FungibleTokenService cip68FungibleTokenService;

    private final Cip68FTDatumParser cip68DatumParser;

    private final MetadataReferenceNftRepository metadataReferenceNftRepository;

    @EventListener
    public void processTransaction(AddressUtxoEvent addressUtxoEvent) {
        var slot = addressUtxoEvent.getEventMetadata().getSlot();
        addressUtxoEvent.getTxInputOutputs()
                .stream()
                .flatMap(txInputOutput -> txInputOutput.getOutputs().stream())
                .flatMap(this::findReferenceNft)
                .flatMap(this::parseDatum)
                .filter(this::isValidFTMetadata)
                .forEach(referenceNftUtxoData -> {
                    var assetType = referenceNftUtxoData.referenceNft();
                    var metadata = referenceNftUtxoData.fungibleTokenMetadata();
                    var referenceNftEntity = buildMetadataReferenceNft(metadata, assetType, referenceNftUtxoData.datum(), slot);
                    metadataReferenceNftRepository.save(referenceNftEntity);
                });
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

    private Stream<ReferenceNftUtxoData> parseDatum(Pair<Amt, AddressUtxo> referenceNftUtxo) {
        return cip68DatumParser.parse(referenceNftUtxo.second().getInlineDatum())
                .stream()
                .map(fungibleTokenMetadata -> new ReferenceNftUtxoData(AssetType.fromUnit(referenceNftUtxo.first().getUnit()),
                        fungibleTokenMetadata,
                        referenceNftUtxo.second().getInlineDatum()));
    }

    private Stream<Pair<Amt, AddressUtxo>> findReferenceNft(AddressUtxo utxo) {
        return cip68FungibleTokenService.extractReferenceNft(utxo).map(amt -> new Pair<>(amt, utxo)).stream();
    }


}
