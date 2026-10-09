package org.cardanofoundation.tokenmetadata.registry.api.service;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.utxo.domain.AddressUtxoEvent;
import com.bloxbean.cardano.yaci.store.utxo.domain.TxInputOutput;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip68.ParsedCip68Datum;
import org.cardanofoundation.tokenmetadata.registry.api.util.AssetType;
import org.cardanofoundation.tokenmetadata.registry.entity.MetadataReferenceNft;
import org.cardanofoundation.tokenmetadata.registry.repository.MetadataReferenceNftRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.cardanofoundation.tokenmetadata.registry.api.model.cip68.Cip68Constants.FUNGIBLE_TOKEN_PREFIX;
import static org.cardanofoundation.tokenmetadata.registry.api.model.cip68.Cip68Constants.LABEL_FT;
import static org.cardanofoundation.tokenmetadata.registry.api.model.cip68.Cip68Constants.LABEL_NFT;
import static org.cardanofoundation.tokenmetadata.registry.api.model.cip68.Cip68Constants.LABEL_RFT;
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
    @Transactional
    public void handleRollback(RollbackEvent rollbackEvent) {
        long rollbackSlot = rollbackEvent.getRollbackTo().getSlot();
        int count = metadataReferenceNftRepository.deleteBySlotGreaterThan(rollbackSlot);
        log.info("CIP-68 rollback to slot {}: deleted {} reference NFT records", rollbackSlot, count);
    }

    @EventListener
    public void processTransaction(AddressUtxoEvent addressUtxoEvent) {
        Long slot = addressUtxoEvent.getMetadata().getSlot();
        List<MetadataReferenceNft> entities = new ArrayList<>();
        // Per transaction: the labels of a reference NFT come from the user tokens paired with it, which are minted in
        // the same transaction
        for (TxInputOutput txInputOutput : addressUtxoEvent.getTxInputOutputs()) {
            Set<String> assetUnitsInTx = collectAssetUnits(txInputOutput);
            for (AddressUtxo output : txInputOutput.getOutputs()) {
                for (Amt referenceNftAmt : referenceNftsToIndex(output)) {
                    AssetType referenceNft = AssetType.fromUnit(referenceNftAmt.getUnit());
                    cip68DatumParser.parse(output.getInlineDatum(), referenceNft)
                            .filter(datum -> isIndexable(datum, referenceNft, assetUnitsInTx))
                            .ifPresent(datum -> entities.add(
                                    buildMetadataReferenceNft(datum, referenceNft, output.getInlineDatum(), slot)));
                }
            }
        }

        if (!entities.isEmpty()) {
            metadataReferenceNftRepository.saveAll(entities);
        }
    }

    /**
     * The reference NFTs of an output whose datum should be indexed: all of them. An output with one reference NFT is
     * the normal case. With several, a nested datum carries the metadata of each, each resolved to its own entry. A
     * flat datum has no entry per token, but CIP-68's retrieval steps look up the output the reference NFT is locked in
     * and read its datum, whatever else the output holds, so a flat datum is the metadata of every reference NFT in the
     * output. They are all indexed with it, and one warning lists them so the case can be audited.
     */
    private List<Amt> referenceNftsToIndex(AddressUtxo output) {
        List<Amt> referenceNfts = cip68FungibleTokenService.extractReferenceNfts(output);
        if (referenceNfts.size() > 1 && !cip68DatumParser.hasNestedMetadata(output.getInlineDatum())) {
            log.warn("Output {}#{} holds {} reference NFTs with a flat CIP-68 datum; CIP-68 gives a flat datum to every "
                            + "reference NFT in the output, so indexing it for all of them: {}",
                    output.getTxHash(), output.getOutputIndex(), referenceNfts.size(),
                    referenceNfts.stream().map(Amt::getUnit).collect(Collectors.joining(", ")));
        }
        return referenceNfts;
    }

    /**
     * Whether a datum is stored. It has to satisfy what CIP-68 requires for every label of its reference NFT (see
     * {@link Cip68FungibleTokenService#invalidReason}); the first label it fails is logged at WARN with the reason, and
     * the datum is not stored. Only fungible tokens are served, and the registry stores a description with every row,
     * so a valid datum of a 222 NFT or a 444 RFT without one is not stored either, at debug: it is not a fungible token.
     */
    private boolean isIndexable(ParsedCip68Datum datum, AssetType referenceNft, Set<String> assetUnitsInTx) {
        List<Integer> labels = deriveLabels(referenceNft, assetUnitsInTx, datum);
        for (int label : labels) {
            Optional<String> invalidReason = cip68FungibleTokenService.invalidReason(datum, label);
            if (invalidReason.isPresent()) {
                log.warn("Skipping CIP-68 datum of {}/{} (label {}{}): {}", referenceNft.policyId(),
                        referenceNft.assetName(), label, labels.size() > 1 ? ", user tokens " + labels : "",
                        invalidReason.get());
                return false;
            }
        }
        if (datum.description() == null) {
            log.debug("Skipping CIP-68 datum of {}/{} (label {}): no description, and not a fungible token",
                    referenceNft.policyId(), referenceNft.assetName(), labels.getFirst());
            return false;
        }
        return true;
    }

    /**
     * The CIP-68 user-token labels of a reference NFT, from the user tokens paired with it. CIP-68 pairs them by policy
     * and base name ({@code 000643b0 + base} and {@code <label> + base}), so only a user token of the same policy and
     * base name in the same transaction counts. CIP-68 allows one reference NFT to have user tokens of several labels,
     * and then the datum has to satisfy the requirements of each; they are returned in the order 222, 333, 444. Without
     * a paired user token (it was minted in another transaction), the label is inferred from the datum, see
     * {@link #inferLabelFromDatum}, and it is the only one.
     */
    private static List<Integer> deriveLabels(AssetType referenceNft, Set<String> assetUnitsInTx, ParsedCip68Datum datum) {
        String baseName = referenceNft.assetName().substring(REFERENCE_TOKEN_PREFIX.length());
        List<Integer> labels = new ArrayList<>();
        if (hasPairedUserToken(assetUnitsInTx, referenceNft, NFT_TOKEN_PREFIX, baseName)) {
            labels.add(LABEL_NFT);
        }
        if (hasPairedUserToken(assetUnitsInTx, referenceNft, FUNGIBLE_TOKEN_PREFIX, baseName)) {
            labels.add(LABEL_FT);
        }
        if (hasPairedUserToken(assetUnitsInTx, referenceNft, RICH_FUNGIBLE_TOKEN_PREFIX, baseName)) {
            labels.add(LABEL_RFT);
        }
        return labels.isEmpty() ? List.of(inferLabelFromDatum(datum)) : labels;
    }

    /**
     * The label of a reference NFT whose user token is not in the transaction. {@code ticker} and {@code logo} are
     * defined only for the 333 fungible token; {@code image}, {@code mediaType} and {@code files} only for the 222 NFT
     * and the 444 RFT, which {@code decimals} tells apart. A datum with none of them is treated as a fungible token, so
     * the fungible token rules apply to it.
     */
    private static int inferLabelFromDatum(ParsedCip68Datum datum) {
        if (datum.ticker() != null || datum.logo() != null) {
            return LABEL_FT;
        }
        if (datum.image() != null || datum.mediaType() != null || datum.hasFiles()) {
            return datum.decimals() != null ? LABEL_RFT : LABEL_NFT;
        }
        return LABEL_FT;
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

    private MetadataReferenceNft buildMetadataReferenceNft(ParsedCip68Datum metadata, AssetType assetType, String datum, Long slot) {
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
