package org.cardanofoundation.tokenmetadata.registry.api.service;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.utxo.domain.AddressUtxoEvent;
import com.bloxbean.cardano.yaci.store.utxo.domain.TxInputOutput;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip68.FungibleTokenMetadata;
import org.cardanofoundation.tokenmetadata.registry.api.util.AssetType;
import org.cardanofoundation.tokenmetadata.registry.entity.MetadataReferenceNft;
import org.cardanofoundation.tokenmetadata.registry.repository.MetadataReferenceNftRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static org.cardanofoundation.tokenmetadata.registry.api.model.cip68.Cip68Constants.FUNGIBLE_TOKEN_PREFIX;
import static org.cardanofoundation.tokenmetadata.registry.api.model.cip68.Cip68Constants.NFT_TOKEN_PREFIX;
import static org.cardanofoundation.tokenmetadata.registry.api.model.cip68.Cip68Constants.REFERENCE_TOKEN_PREFIX;
import static org.cardanofoundation.tokenmetadata.registry.api.model.cip68.Cip68Constants.RICH_FUNGIBLE_TOKEN_PREFIX;

@Service
@RequiredArgsConstructor
@Slf4j
public class Cip68EventListener {

    private final Cip68FungibleTokenService cip68FungibleTokenService;

    private final Cip68FTDatumParser cip68DatumParser;

    private final MetadataReferenceNftRepository metadataReferenceNftRepository;

    @EventListener
    public void processTransaction(AddressUtxoEvent addressUtxoEvent) {
        Long slot = addressUtxoEvent.getMetadata().getSlot();
        // Per transaction: telling whether a skipped datum belongs to a fungible token needs the user token
        // paired with its reference NFT, which is minted in the same transaction
        for (TxInputOutput txInputOutput : addressUtxoEvent.getTxInputOutputs()) {
            Set<String> assetUnitsInTx = collectAssetUnits(txInputOutput);
            for (AddressUtxo output : txInputOutput.getOutputs()) {
                for (Amt referenceNftAmt : referenceNftsToIndex(output)) {
                    AssetType referenceNft = AssetType.fromUnit(referenceNftAmt.getUnit());
                    cip68DatumParser.parse(output.getInlineDatum(), referenceNft).ifPresent(metadata -> {
                        if (cip68FungibleTokenService.isValidFTMetadata(metadata)) {
                            metadataReferenceNftRepository.save(
                                    buildMetadataReferenceNft(metadata, referenceNft, output.getInlineDatum(), slot));
                        } else {
                            reportSkipped(metadata, referenceNft, assetUnitsInTx);
                        }
                    });
                }
            }
        }
    }

    /**
     * The reference NFTs of an output whose datum should be indexed. An output with one reference NFT is the normal
     * case. With several, a nested datum carries the metadata of each, so all of them are indexed, each resolved to
     * its own entry. A flat datum describes a single token and cannot be tied to any of several, so only the first is
     * indexed and the rest are reported, not skipped silently.
     */
    private List<Amt> referenceNftsToIndex(AddressUtxo output) {
        List<Amt> referenceNfts = cip68FungibleTokenService.extractReferenceNfts(output);
        if (referenceNfts.size() <= 1 || cip68DatumParser.hasNestedMetadata(output.getInlineDatum())) {
            return referenceNfts;
        }
        log.warn("Output {}#{} holds {} reference NFTs with a flat CIP-68 datum, which describes one token; "
                        + "indexing only {} and ignoring the other {}",
                output.getTxHash(), output.getOutputIndex(), referenceNfts.size(), referenceNfts.getFirst().getUnit(),
                referenceNfts.size() - 1);
        return List.of(referenceNfts.getFirst());
    }

    /**
     * A datum that is not indexed leaves a trace when it should have been: one without a name, or a fungible token
     * without a description (CIP-68 requires both for a 333 token). Only fungible tokens are served, and CIP-68 does
     * not require a description from NFTs and RFTs, so a description-less datum that is not identifiable as a
     * fungible token is expected and only logged at debug.
     */
    private void reportSkipped(FungibleTokenMetadata metadata, AssetType referenceNft, Set<String> assetUnitsInTx) {
        if (metadata.name() == null) {
            log.warn("Skipping CIP-68 datum of {}/{}: it has no name", referenceNft.policyId(), referenceNft.assetName());
        } else if (isIdentifiableAsFungibleToken(metadata, referenceNft, assetUnitsInTx)) {
            log.warn("Skipping CIP-68 datum of {}/{}: it has no description, which CIP-68 requires for a fungible token",
                    referenceNft.policyId(), referenceNft.assetName());
        } else {
            log.debug("Skipping CIP-68 datum of {}/{}: no description, and not identifiable as a fungible token",
                    referenceNft.policyId(), referenceNft.assetName());
        }
    }

    /**
     * Whether a reference NFT belongs to a 333 fungible token. CIP-68 pairs a reference NFT with its user token by
     * policy and base name ({@code 000643b0 + base} and {@code <label> + base}), so only a user token of the same
     * policy and base name in the same transaction counts; when a 222 NFT and a 333 token are both paired, the 222
     * wins. Without a paired user token (minted in another transaction), the datum counts as a fungible token only if
     * it carries {@code ticker} or {@code logo}, the fields only the 333 token defines; {@code decimals} alone does not
     * decide, since the 444 RFT defines it too.
     */
    private static boolean isIdentifiableAsFungibleToken(FungibleTokenMetadata metadata, AssetType referenceNft,
                                                         Set<String> assetUnitsInTx) {
        String baseName = referenceNft.assetName().substring(REFERENCE_TOKEN_PREFIX.length());
        if (hasPairedUserToken(assetUnitsInTx, referenceNft, NFT_TOKEN_PREFIX, baseName)) {
            return false;
        }
        if (hasPairedUserToken(assetUnitsInTx, referenceNft, FUNGIBLE_TOKEN_PREFIX, baseName)) {
            return true;
        }
        if (hasPairedUserToken(assetUnitsInTx, referenceNft, RICH_FUNGIBLE_TOKEN_PREFIX, baseName)) {
            return false;
        }
        return metadata.ticker() != null || metadata.logo() != null;
    }

    private static boolean hasPairedUserToken(Set<String> assetUnitsInTx, AssetType referenceNft,
                                              String userTokenPrefix, String baseName) {
        return assetUnitsInTx.contains(normalize(referenceNft.policyId() + userTokenPrefix + baseName));
    }

    /** Every asset unit (policy id + asset name) in the transaction's outputs, normalised for comparison. */
    private static Set<String> collectAssetUnits(TxInputOutput txInputOutput) {
        return txInputOutput.getOutputs().stream()
                .flatMap(output -> output.getAmounts().stream())
                .map(amt -> normalize(AssetType.fromUnit(amt.getUnit()).toUnit()))
                .collect(Collectors.toSet());
    }

    private static String normalize(String unit) {
        return unit.toLowerCase(Locale.ROOT);
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

}
