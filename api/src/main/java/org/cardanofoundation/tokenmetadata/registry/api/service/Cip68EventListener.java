package org.cardanofoundation.tokenmetadata.registry.api.service;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.utxo.domain.AddressUtxoEvent;
import com.bloxbean.cardano.yaci.store.utxo.domain.TxInputOutput;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip68.ParseOutcome;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip68.ParsedCip68Datum;
import org.cardanofoundation.tokenmetadata.registry.api.util.AssetType;
import org.cardanofoundation.tokenmetadata.registry.entity.MetadataReferenceNft;
import org.cardanofoundation.tokenmetadata.registry.invalidtoken.InvalidTokenReason;
import org.cardanofoundation.tokenmetadata.registry.invalidtoken.InvalidTokenRecorder;
import org.cardanofoundation.tokenmetadata.registry.invalidtoken.InvalidTokenReport;
import org.cardanofoundation.tokenmetadata.registry.repository.MetadataReferenceNftRepository;
import org.cardanofoundation.tokenmetadata.registry.validation.ValidationError;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

/**
 * Indexes CIP-68 reference NFT datums, as yaci-store's processor does (bloxbean/yaci-store#1220): a datum that can be
 * decoded is stored as written, with what departs from CIP-68 in its {@code validation_errors} (ADR-019), and only what
 * cannot be decoded, or fails unexpectedly, is dropped and recorded in {@code invalid_token} (ADR-020).
 * <p>
 * The registry serves fungible tokens (333) and rich fungible tokens (444) only, so a datum whose reference NFT is
 * minted with a 222 NFT and no 333 or 444 token is skipped at debug, unless it updates a reference NFT already stored.
 * A datum without a user token in its transaction is stored: its 333 or 444 token can be minted in another transaction,
 * before or after it, and only the transaction itself can be relied on (blocks are processed in parallel during sync).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class Cip68EventListener {

    /**
     * Writes the {@code properties} JSON once, early, so that a value Jackson refuses is found while the output is
     * built: Hibernate writes it at flush, inside the block transaction, where an exception stops the whole sync
     * (bloxbean/yaci-store#1240, #1243). Do not remove it as "unused serialization".
     */
    private static final ObjectMapper PROPERTIES_MAPPER = new ObjectMapper();
    private static final int DATUM_PREFIX_CHARS = 256;

    private final Cip68FungibleTokenService cip68FungibleTokenService;
    private final Cip68FTDatumParser cip68DatumParser;
    private final MetadataReferenceNftRepository metadataReferenceNftRepository;
    private final InvalidTokenRecorder invalidTokenRecorder;

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
                try {
                    indexOutput(output, assetUnitsInTx, slot, entities);
                } catch (RuntimeException e) {
                    skipOutput(output, slot, e);
                }
            }
        }

        if (!entities.isEmpty()) {
            metadataReferenceNftRepository.saveAll(entities);
        }
    }

    /**
     * Indexes all the reference NFTs of an output. With several, a nested datum carries the metadata of each, each
     * resolved to its own entry; a flat datum is the metadata of every reference NFT of the output (CIP-68's retrieval
     * steps read the datum of the output, whatever else it holds), and one info line lists them so the case can be
     * audited. One reference NFT failing does not take the others of the output with it.
     */
    private void indexOutput(AddressUtxo output, Set<String> assetUnitsInTx, Long slot, List<MetadataReferenceNft> entities) {
        List<Amt> referenceNfts = cip68FungibleTokenService.extractReferenceNfts(output);
        SharedDatum shared = null;
        if (referenceNfts.size() > 1 && !cip68DatumParser.hasNestedMetadata(output.getInlineDatum())) {
            log.info("Output {}#{} holds {} reference NFTs with a flat CIP-68 datum; CIP-68 gives a flat datum to every "
                            + "reference NFT in the output, so indexing it for all of them: {}",
                    output.getTxHash(), output.getOutputIndex(), referenceNfts.size(),
                    referenceNfts.stream().map(Amt::getUnit).collect(Collectors.joining(", ")));
            shared = new SharedDatum(cip68DatumParser, output, referenceNfts.size());
        }
        for (Amt referenceNftAmt : referenceNfts) {
            AssetType referenceNft = AssetType.fromUnit(referenceNftAmt.getUnit());
            try {
                toEntity(output, referenceNft, assetUnitsInTx, slot, shared).ifPresent(entities::add);
            } catch (RuntimeException e) {
                skipReferenceNft(output, slot, referenceNft, e);
            }
        }
    }

    /**
     * The row for one reference NFT of an output, or empty if its datum is not stored. A datum is stored whatever is
     * wrong with it; one that cannot be decoded is recorded in {@code invalid_token}, once for a shared flat datum.
     */
    private Optional<MetadataReferenceNft> toEntity(AddressUtxo output, AssetType referenceNft, Set<String> assetUnitsInTx,
                                                    Long slot, @Nullable SharedDatum shared) {
        ParseOutcome outcome = shared != null
                ? shared.outcome
                : cip68DatumParser.parseDetailed(output.getInlineDatum(), referenceNft);
        if (outcome instanceof ParseOutcome.Parsed(ParsedCip68Datum parsed)) {
            return indexDatum(parsed, output, referenceNft, assetUnitsInTx, slot, shared);
        }
        if (outcome instanceof ParseOutcome.Dropped(String reasonCode, String message) && (shared == null || shared.markReported())) {
            recordDropped(output, slot, referenceNft, reasonCode, message, shared);
        }
        return Optional.empty();
    }

    /**
     * The row for a datum that was read: what is wrong with the datum itself, and what is reported about it. What the
     * label requires is not stored: it is checked when the token is served, for the label asked for
     * ({@link Cip68Validator#validate}).
     */
    private Optional<MetadataReferenceNft> indexDatum(ParsedCip68Datum parsed, AddressUtxo output, AssetType referenceNft,
                                                      Set<String> assetUnitsInTx, Long slot, @Nullable SharedDatum shared) {
        // A reference NFT can have user tokens of several labels (the CIP allows it)
        List<Integer> pairedLabels = pairedLabels(referenceNft, assetUnitsInTx);
        // Without a user token in the transaction the label can only be inferred from the datum, and a datum with none
        // of the CIP-68 token fields (a dApp's own state under the 000643b0 prefix, an empty map) is not token metadata:
        // it would only be indexed as a 333 fungible token by default. Once a reference NFT is indexed, every later
        // datum is, so that an owner who clears the metadata is not served the old one.
        if (pairedLabels.isEmpty() && !parsed.hasTokenFields()
                && !metadataReferenceNftRepository.existsByPolicyIdAndAssetName(referenceNft.policyId(), referenceNft.assetName())) {
            log.debug("Skipping CIP-68 datum of {}/{}: no user token in the transaction and none of the CIP-68 token fields",
                    referenceNft.policyId(), referenceNft.assetName());
            return Optional.empty();
        }
        // Only the user tokens of the transaction tell what a datum is: one without them is stored, since its 333 or 444
        // token can be minted in another transaction (an RFT datum without decimals looks like an NFT's)
        if (!pairedLabels.isEmpty() && !pairedLabels.contains(LABEL_FT) && !pairedLabels.contains(LABEL_RFT)
                && !metadataReferenceNftRepository.existsByPolicyIdAndAssetName(referenceNft.policyId(), referenceNft.assetName())) {
            log.debug("Skipping CIP-68 datum of {}/{} (labels {}): not a fungible or rich fungible token",
                    referenceNft.policyId(), referenceNft.assetName(), pairedLabels);
            return Optional.empty();
        }

        // what is wrong with the datum itself does not depend on the label: the parser's errors, and the logo
        List<ValidationError> errors = new ArrayList<>(parsed.validationErrors());
        ValidationError logoError = Cip68Validator.validateLogo(parsed);
        if (logoError != null) {
            errors.add(logoError);
        }

        MetadataReferenceNft entity = buildMetadataReferenceNft(parsed, errors, referenceNft, output.getInlineDatum(), slot);
        requireSerializableProperties(entity);
        // what the datum itself gets wrong is the same for every reference NFT of a shared flat datum: warn once
        if (shared == null || shared.markReported()) {
            warnPropertiesDroppedAsAttack(output, referenceNft, errors);
        }
        if (!errors.isEmpty()) {
            log.debug("CIP-68 datum of {}/{} (labels {}) is indexed with {} validation errors: {}",
                    referenceNft.policyId(), referenceNft.assetName(), pairedLabels, errors.size(), errors);
        }
        if (pairedLabels.size() > 1) {
            log.info("CIP-68 reference NFT {}/{} has user tokens for labels {}; each token is checked for its own label when "
                            + "it is served",
                    referenceNft.policyId(), referenceNft.assetName(), pairedLabels);
        }
        return Optional.of(entity);
    }

    /**
     * A property nested more than 100 levels is dropped as an attack (ADR-019): one WARN per property names the token and
     * the transaction, so that the attempt can be found afterwards.
     */
    private void warnPropertiesDroppedAsAttack(AddressUtxo output, AssetType referenceNft, List<ValidationError> datumErrors) {
        for (ValidationError error : datumErrors) {
            if (Cip68FTDatumParser.TOO_DEEP.equals(error.code())) {
                log.warn("Output {}#{}: dropping property '{}' of the CIP-68 datum of {}/{} and keeping the rest, because its "
                                + "value is nested deeper than {} levels, this looks like an attack",
                        output.getTxHash(), output.getOutputIndex(), error.fieldName(), referenceNft.policyId(),
                        referenceNft.assetName(), Cip68FTDatumParser.MAX_PROPERTY_DEPTH);
            }
        }
    }

    /** Throws if the {@code properties} cannot be written as JSON, so that the reference NFT is skipped, not the sync. */
    private static void requireSerializableProperties(MetadataReferenceNft entity) {
        if (entity.getProperties() == null) {
            return;
        }
        try {
            PROPERTIES_MAPPER.writeValueAsString(entity.getProperties());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("the properties cannot be written as JSON: " + e.getOriginalMessage(), e);
        }
    }

    /**
     * An unexpected exception while indexing a reference NFT means the datum is something the parser did not foresee,
     * and anyone can put a datum on-chain. This listener runs inside yaci-store's block transaction, so an exception would
     * stop the whole sync (bloxbean/yaci-store#1243): the reference NFT is skipped with a warning and recorded in
     * {@code invalid_token}, and the sync goes on. A failure that only shows at flush or commit is not caught here.
     */
    private void skipReferenceNft(AddressUtxo output, Long slot, AssetType referenceNft, RuntimeException e) {
        log.warn("Output {}#{}: skipping the CIP-68 datum of {}/{}, indexing it failed unexpectedly ({}: {}); datum {}",
                output.getTxHash(), output.getOutputIndex(), referenceNft.policyId(), referenceNft.assetName(),
                e.getClass().getSimpleName(), e.getMessage(), output.getInlineDatum());
        log.debug("Stack trace of the CIP-68 indexing failure of output {}#{}", output.getTxHash(), output.getOutputIndex(), e);
        recordProcessingFailure(output, slot, referenceNft, e);
    }

    /** The same for a failure that cannot be put on one reference NFT: the whole output is skipped. */
    private void skipOutput(AddressUtxo output, Long slot, RuntimeException e) {
        log.warn("Output {}#{}: skipping the CIP-68 datum, indexing it failed unexpectedly ({}: {}); datum {}",
                output.getTxHash(), output.getOutputIndex(), e.getClass().getSimpleName(), e.getMessage(),
                output.getInlineDatum());
        log.debug("Stack trace of the CIP-68 indexing failure of output {}#{}", output.getTxHash(), output.getOutputIndex(), e);
        List<Amt> referenceNfts;
        try {
            referenceNfts = cip68FungibleTokenService.extractReferenceNfts(output);
        } catch (RuntimeException _) {
            referenceNfts = List.of();
        }
        if (referenceNfts.isEmpty()) {
            recordProcessingFailure(output, slot, null, e);
        }
        for (Amt referenceNft : referenceNfts) {
            recordProcessingFailure(output, slot, AssetType.fromUnit(referenceNft.getUnit()), e);
        }
    }

    private void recordProcessingFailure(AddressUtxo output, Long slot, @Nullable AssetType referenceNft, RuntimeException e) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("exception", e.getClass().getName());
        details.put("message", String.valueOf(e.getMessage()));
        details.put("datum_prefix", abbreviateDatum(output.getInlineDatum()));
        invalidTokenRecorder.report(droppedReport(output, slot, referenceNft, InvalidTokenReason.PROCESSING_FAILURE,
                "indexing the output failed unexpectedly: " + e.getClass().getSimpleName() + ": " + e.getMessage(), details));
    }

    private void recordDropped(AddressUtxo output, Long slot, AssetType referenceNft, String reasonCode, String message,
                               @Nullable SharedDatum shared) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("datum_prefix", abbreviateDatum(output.getInlineDatum()));
        details.put("datum_bytes", output.getInlineDatum() == null ? 0 : output.getInlineDatum().length() / 2);
        if (shared != null) {
            details.put("reference_nfts_in_output", shared.referenceNfts);
        }
        invalidTokenRecorder.report(droppedReport(output, slot, referenceNft, reasonCode, message, details));
    }

    private static InvalidTokenReport droppedReport(AddressUtxo output, Long slot, @Nullable AssetType referenceNft,
                                                    String reasonCode, String reason, Map<String, Object> details) {
        return InvalidTokenReport.builder()
                .standard(InvalidTokenReason.CIP_68)
                .reasonCode(reasonCode)
                .reason(reason)
                .policyId(referenceNft == null ? null : referenceNft.policyId())
                .assetName(referenceNft == null ? null : referenceNft.assetName())
                .subject(referenceNft == null ? null : referenceNft.toUnit())
                .slot(slot)
                .txHash(output.getTxHash())
                .outputIndex(output.getOutputIndex())
                .details(details)
                .build();
    }

    /** A datum can be 16 KB: the table keeps its start, which is enough to find it. */
    private static String abbreviateDatum(@Nullable String datum) {
        if (datum == null) {
            return "";
        }
        return datum.length() <= DATUM_PREFIX_CHARS ? datum : datum.substring(0, DATUM_PREFIX_CHARS) + "...";
    }

    /**
     * The flat datum of an output that holds several reference NFTs. It is the metadata of each of them and is the same
     * for all, so it is parsed once, and what is wrong with it is reported once for the output. The rows and the checks
     * against the labels stay per reference NFT, since each has its own row and its own user tokens.
     */
    private static final class SharedDatum {
        private final int referenceNfts;
        private final ParseOutcome outcome;
        private boolean reported;

        private SharedDatum(Cip68FTDatumParser parser, AddressUtxo output, int referenceNfts) {
            this.referenceNfts = referenceNfts;
            // Parsed here, once, without a reference NFT: the datum is not specific to one of them.
            this.outcome = parser.parseDetailed(output.getInlineDatum(), null);
        }

        /** True the first time it is called: what the shared datum gets wrong is reported by the first reference NFT. */
        private boolean markReported() {
            boolean first = !reported;
            reported = true;
            return first;
        }
    }

    /**
     * The CIP-68 user-token labels of a reference NFT, from the user tokens paired with it. CIP-68 pairs them by policy
     * and base name ({@code 000643b0 + base} and {@code <label> + base}), so only a user token of the same policy and
     * base name in the same transaction counts. CIP-68 allows one reference NFT to have user tokens of several labels;
     * they are returned in the order 222, 333, 444. Empty without a paired
     * user token (it was minted in another transaction).
     */
    private static List<Integer> pairedLabels(AssetType referenceNft, Set<String> assetUnitsInTx) {
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
        return labels;
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

    static MetadataReferenceNft buildMetadataReferenceNft(ParsedCip68Datum parsed, List<ValidationError> errors,
                                                                  AssetType assetType, String datum, Long slot) {
        return MetadataReferenceNft.builder()
                .policyId(assetType.policyId())
                .assetName(assetType.assetName())
                .slot(slot)
                .name(parsed.name())
                .description(parsed.description())
                .ticker(parsed.ticker())
                .url(parsed.url())
                .decimals(parsed.decimals())
                .logo(parsed.logo())
                .image(parsed.image())
                .mediaType(parsed.mediaType())
                .version(parsed.version())
                .datum(datum)
                .properties(parsed.properties())
                .validationErrors(errors)
                .build();
    }

    /** What is wrong with the datum itself, whatever its label: the parser's errors, and the logo. */
    static List<ValidationError> datumErrors(ParsedCip68Datum parsed) {
        List<ValidationError> errors = new ArrayList<>(parsed.validationErrors());
        ValidationError logoError = Cip68Validator.validateLogo(parsed);
        if (logoError != null) {
            errors.add(logoError);
        }
        return errors;
    }

    /**
     * Every error of a datum checked against each of its labels, without storing any of them: what the validate endpoint
     * reports, so that it answers exactly as the registry would serve the token.
     */
    static List<ValidationError> errorsForLabels(ParsedCip68Datum parsed, List<Integer> labels) {
        List<ValidationError> datumErrors = datumErrors(parsed);
        if (labels.isEmpty()) {
            return new ArrayList<>(datumErrors);
        }
        // A description written in a form the label judges (a wrong type, chunks, bytes that are not UTF-8) is stored
        // without a label: validate() reports it again with the category of the label asked for, so it is left out here.
        List<ValidationError> stored = datumErrors.stream().filter(error -> !Cip68Validator.isDescriptionForm(error)).toList();
        List<ValidationError> errors = new ArrayList<>(stored);
        // Cip68Validator.validate reads the row that would be stored: build it from the parsed fields, without the
        // identity fields (policy, asset name, slot) it does not look at.
        MetadataReferenceNft row = MetadataReferenceNft.builder()
                .name(parsed.name())
                .description(parsed.description())
                .image(parsed.image())
                .validationErrors(datumErrors)
                .build();
        for (int label : labels) {
            // validate() returns the stored errors as they are, so they would come back once per label
            for (ValidationError error : Cip68Validator.validate(row, label)) {
                if (!stored.contains(error)) {
                    errors.add(error);
                }
            }
        }
        return errors;
    }
}
