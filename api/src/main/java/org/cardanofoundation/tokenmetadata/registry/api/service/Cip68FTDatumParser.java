package org.cardanofoundation.tokenmetadata.registry.api.service;

import com.bloxbean.cardano.client.exception.CborDeserializationException;
import com.bloxbean.cardano.client.plutus.spec.*;
import com.bloxbean.cardano.client.util.HexUtil;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip68.Cip68Constants;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip68.FungibleTokenMetadata;
import org.cardanofoundation.tokenmetadata.registry.api.util.AssetType;
import org.cardanofoundation.tokenmetadata.registry.util.TokenDecimals;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class Cip68FTDatumParser {

    public static final String DECIMALS = "decimals";
    public static final String DESCRIPTION = "description";
    public static final String LOGO = "logo";
    public static final String NAME = "name";
    public static final String TICKER = "ticker";
    public static final String URL = "url";

    /** CIP-68 version 4 wraps the metadata in a CIP-25 style map: {"721": {policy_id: {asset_name: metadata}}}. */
    private static final BytesPlutusData NESTED_MAP_KEY = BytesPlutusData.of("721");
    private static final long NESTED_MAP_MIN_VERSION = 4;

    /**
     * Manually parses Cip68 Fungible Token Datum
     *
     * @param inlineDatum the hex encoded datum
     * @return the Cip68 Fungible Token Metadata
     */
    public Optional<FungibleTokenMetadata> parse(String inlineDatum) {
        return parse(inlineDatum, null);
    }

    /**
     * Same as {@link #parse(String)}, but resolves a version 4 nested-map datum to the entry for
     * {@code referenceNft}. Without it, a nested map is only resolved when it has a single entry.
     */
    public Optional<FungibleTokenMetadata> parse(String inlineDatum, @Nullable AssetType referenceNft) {
        if (inlineDatum == null || inlineDatum.isBlank()) {
            return Optional.empty();
        }

        try {
            return extractDatumParts(inlineDatum)
                    .flatMap(parts -> resolveMetadata(parts, referenceNft)
                            .map(properties -> buildMetadata(properties, parts.version())));
        } catch (StackOverflowError _) {
            // TODO: temporary workaround. Remove once cardano-client-lib decodes CBOR without
            //  recursion (bloxbean/cardano-client-lib#681).
            // The CBOR decoder recurses once per nesting level, and the ledger bounds a datum only by
            // transaction size, so a valid on-chain datum can be nested deeper than the stack allows.
            // StackOverflowError is an Error, not an Exception, so it needs its own catch: skip the
            // datum like any other undecodable one.
            log.warn("Skipping CIP-68 datum nested too deeply to decode ({} bytes)", inlineDatum.length() / 2);
            return Optional.empty();
        } catch (Exception e) {
            // One line per failure, with the datum for reproduction; the stack trace only at DEBUG,
            // so a run of unparseable datums doesn't flood the sync log.
            log.warn("Skipping unparseable CIP-68 datum ({}): {}", e, inlineDatum);
            log.debug("CIP-68 datum parse failure", e);
            return Optional.empty();
        }
    }

    /**
     * Strip the CIP-68 datum envelope: a Constr containing a {@code (Map, BigInt)} pair
     * (the metadata properties map and the version integer). Returns empty for any datum
     * that doesn't fit this shape.
     */
    private Optional<DatumParts> extractDatumParts(String inlineDatum) throws CborDeserializationException {
        PlutusData plutusData = PlutusData.deserialize(HexUtil.decodeHexString(inlineDatum));

        if (!(plutusData instanceof ConstrPlutusData cip68Data)) {
            return Optional.empty();
        }

        List<PlutusData> dataList = cip68Data.getData().getPlutusDataList();
        if (dataList.size() < 2 || !(dataList.getFirst() instanceof MapPlutusData properties)) {
            return Optional.empty();
        }

        if (!(dataList.get(1) instanceof BigIntPlutusData version)) {
            return Optional.empty();
        }

        // version is required and stored as a long, but a datum integer is unbounded: reject values
        // that don't fit rather than let longValue() silently wrap them (2^64 + 1 would become 1)
        BigInteger versionValue = version.getValue();
        if (versionValue.bitLength() >= Long.SIZE) {
            log.warn("Ignoring CIP-68 datum with out-of-range version {}", versionValue);
            return Optional.empty();
        }

        return Optional.of(new DatumParts(properties, versionValue.longValue()));
    }

    /**
     * Returns the metadata map to read fields from. Versions 1–3 carry it directly; version 4 may nest it
     * under {@code "721" -> policy_id -> asset_name} (asset name without the label prefix, both as raw
     * bytes). A version 4 datum without the {@code "721"} key is read directly.
     */
    private Optional<MapPlutusData> resolveMetadata(DatumParts parts, @Nullable AssetType referenceNft) {
        MapPlutusData properties = parts.properties();
        if (parts.version() < NESTED_MAP_MIN_VERSION
                || !(properties.getMap().get(NESTED_MAP_KEY) instanceof MapPlutusData byPolicy)) {
            return Optional.of(properties);
        }

        if (referenceNft == null) {
            // No asset context: only an unambiguous single entry can be resolved
            return singleValue(byPolicy)
                    .flatMap(Cip68FTDatumParser::singleValue);
        }

        String assetNameWithoutLabel = referenceNft.assetName().substring(Cip68Constants.REFERENCE_TOKEN_PREFIX.length());
        return asMap(byPolicy.getMap().get(BytesPlutusData.of(HexUtil.decodeHexString(referenceNft.policyId()))))
                .flatMap(byAsset -> asMap(byAsset.getMap().get(BytesPlutusData.of(HexUtil.decodeHexString(assetNameWithoutLabel)))));
    }

    private static Optional<MapPlutusData> singleValue(MapPlutusData map) {
        return map.getMap().size() == 1 ? asMap(map.getMap().values().iterator().next()) : Optional.empty();
    }

    private static Optional<MapPlutusData> asMap(@Nullable PlutusData data) {
        return data instanceof MapPlutusData map ? Optional.of(map) : Optional.empty();
    }

    private FungibleTokenMetadata buildMetadata(MapPlutusData properties, long version) {
        return new FungibleTokenMetadata(getDecimalsProperty(properties).orElse(null),
                getStringProperty(DESCRIPTION, properties).orElse(null),
                getStringOrChunkedProperty(LOGO, properties).orElse(null),
                getStringProperty(NAME, properties).orElse(null),
                getStringProperty(TICKER, properties).orElse(null),
                getStringProperty(URL, properties).orElse(null),
                version);
    }

    /** Internal record for the unwrapped CIP-68 envelope (properties Map, range-checked version). */
    private record DatumParts(MapPlutusData properties, long version) {
    }

    private Optional<String> getStringProperty(String propertyName, MapPlutusData mapPlutusData) {
        PlutusData property = mapPlutusData.getMap().get(BytesPlutusData.of(propertyName));
        if (property instanceof BytesPlutusData bytes) {
            return Optional.of(bytesToString(bytes.getValue()));
        } else {
            return Optional.empty();
        }
    }

    /**
     * CIP-68 FT {@code logo} is a {@code uri = bounded_bytes / [* bounded_bytes]}: a value longer than
     * 64 bytes may be split into a list of byte-string chunks. This joins them back together, and
     * falls back to {@link #getStringProperty} for the single byte-string case.
     */
    private Optional<String> getStringOrChunkedProperty(String propertyName, MapPlutusData mapPlutusData) {
        PlutusData property = mapPlutusData.getMap().get(BytesPlutusData.of(propertyName));
        if (property instanceof ListPlutusData list) {
            StringBuilder sb = new StringBuilder();
            for (PlutusData chunk : list.getPlutusDataList()) {
                if (chunk instanceof BytesPlutusData bytes) {
                    sb.append(bytesToString(bytes.getValue()));
                }
            }
            return sb.isEmpty() ? Optional.empty() : Optional.of(sb.toString());
        }
        return getStringProperty(propertyName, mapPlutusData);
    }

    /**
     * Reads {@code decimals} from the datum. The value is an unbounded on-chain integer, so values that
     * don't fit in a {@code long} are rejected before narrowing: {@code longValue()} alone would
     * silently wrap values above {@code 2^63} into a plausible-looking number. An out-of-range value
     * is dropped (the rest of the metadata is kept) rather than stored.
     */
    private Optional<Long> getDecimalsProperty(MapPlutusData mapPlutusData) {
        PlutusData property = mapPlutusData.getMap().get(BytesPlutusData.of(DECIMALS));
        if (!(property instanceof BigIntPlutusData bigInt)) {
            return Optional.empty();
        }

        BigInteger value = bigInt.getValue();
        // bitLength guard first: longValue() is only exact when the value fits in a long
        if (value.bitLength() >= Long.SIZE || !TokenDecimals.isInRange(value.longValue())) {
            log.warn("Ignoring out-of-range CIP-68 decimals {} (allowed {})", value, TokenDecimals.RANGE);
            return Optional.empty();
        }

        return Optional.of(value.longValue());
    }

    private static String bytesToString(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8).replace("\0", "");
    }

}
