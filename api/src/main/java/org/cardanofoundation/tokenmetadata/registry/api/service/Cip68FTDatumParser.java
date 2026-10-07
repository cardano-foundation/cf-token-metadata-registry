package org.cardanofoundation.tokenmetadata.registry.api.service;

import com.bloxbean.cardano.client.exception.CborDeserializationException;
import com.bloxbean.cardano.client.plutus.spec.*;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.common.util.StringUtil;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip68.Cip68Constants;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip68.FungibleTokenMetadata;
import org.cardanofoundation.tokenmetadata.registry.api.util.AssetType;
import org.cardanofoundation.tokenmetadata.registry.util.TokenDecimals;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
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

    /** Largest {@code logo} accepted, in bytes. Same limit as the CIP-26 logo. */
    public static final int LOGO_MAX_BYTES = 64 * 1024;

    /** The nested metadata format wraps the metadata in a CIP-25 style map: {"721": {policy_id: {asset_name: metadata}}}. */
    private static final BytesPlutusData NESTED_MAP_KEY = BytesPlutusData.of("721");

    /**
     * The datum versions CIP-68 defines. They are informational: how a datum is read depends on its structure (see
     * {@link #isNested}), as the CIP's retrieval steps say, not on its version. A datum with another version is still
     * read, and a warning is logged so a new version is noticed. Update the upper bound when the CIP adds a version.
     */
    static final long MIN_DEFINED_VERSION = 1;
    static final long MAX_DEFINED_VERSION = 4;

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
                    .map(parts -> warnIfVersionNotDefined(parts, referenceNft))
                    .flatMap(parts -> resolveMetadata(parts, referenceNft)
                            .map(properties -> buildMetadata(properties, parts.version())));
        } catch (StackOverflowError _) {
            // Temporary workaround: remove once cardano-client-lib decodes CBOR without recursion
            // (bloxbean/cardano-client-lib#681).
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
     * Returns the metadata map to read fields from. A flat datum carries it directly; a nested one under
     * {@code "721" -> policy_id -> asset_name} (asset name without the label prefix, both as raw bytes).
     */
    private Optional<MapPlutusData> resolveMetadata(DatumParts parts, @Nullable AssetType referenceNft) {
        MapPlutusData properties = parts.properties();
        if (!isNested(parts)) {
            return Optional.of(properties);
        }
        MapPlutusData byPolicy = (MapPlutusData) properties.getMap().get(NESTED_MAP_KEY);

        if (referenceNft == null) {
            // No asset context: only an unambiguous single entry can be resolved
            return singleValue(byPolicy)
                    .flatMap(Cip68FTDatumParser::singleValue);
        }

        String assetNameWithoutLabel = referenceNft.assetName().substring(Cip68Constants.REFERENCE_TOKEN_PREFIX.length());
        return asMap(byPolicy.getMap().get(BytesPlutusData.of(HexUtil.decodeHexString(referenceNft.policyId()))))
                .flatMap(byAsset -> asMap(byAsset.getMap().get(BytesPlutusData.of(HexUtil.decodeHexString(assetNameWithoutLabel)))));
    }

    /**
     * A datum whose version CIP-68 does not define (1 to 4) is read like any other, by its structure, and a warning
     * names the token. Not a failure: the datum decoded fine (on mainnet, Greenland Reserve Coin declares version 0).
     * The version is stored as written.
     */
    private static DatumParts warnIfVersionNotDefined(DatumParts parts, @Nullable AssetType referenceNft) {
        long version = parts.version();
        if (version >= MIN_DEFINED_VERSION && version <= MAX_DEFINED_VERSION) {
            return parts;
        }
        if (referenceNft != null) {
            log.warn("CIP-68 datum of {}/{} has version {}, which CIP-68 does not define ({} to {}); reading it by its structure",
                    referenceNft.policyId(), referenceNft.assetName(), version, MIN_DEFINED_VERSION, MAX_DEFINED_VERSION);
        } else {
            log.warn("CIP-68 datum has version {}, which CIP-68 does not define ({} to {}); reading it by its structure",
                    version, MIN_DEFINED_VERSION, MAX_DEFINED_VERSION);
        }
        return parts;
    }

    /**
     * Whether the metadata map is in the nested format: it has the {@code "721"} key, whose value is a map. This is the
     * test in step 4 of the CIP's retrieval steps ("direct metadata (map without "721" key) or nested map format (map
     * with "721" key)"), and it does not depend on the version. A flat map with an additional property named
     * {@code "721"} that holds a map would be misread; none exists on mainnet.
     */
    private static boolean isNested(DatumParts parts) {
        return parts.properties().getMap().get(NESTED_MAP_KEY) instanceof MapPlutusData;
    }

    /**
     * Whether the datum is in the nested format, which can carry the metadata of several reference NFTs. A flat datum
     * describes one token. Anything that is not a CIP-68 datum, or cannot be decoded, is not nested.
     */
    public boolean hasNestedMetadata(@Nullable String inlineDatum) {
        if (inlineDatum == null || inlineDatum.isBlank()) {
            return false;
        }
        try {
            return extractDatumParts(inlineDatum).map(Cip68FTDatumParser::isNested).orElse(false);
        } catch (Exception | StackOverflowError _) {
            return false;
        }
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

    /**
     * Reads a text property. CIP-68 text is UTF-8, so valid UTF-8 is stored as text. Bytes that are not valid UTF-8
     * are stored as hex instead of being decoded with replacement characters, which would lose the original bytes.
     */
    private Optional<String> getStringProperty(String propertyName, MapPlutusData mapPlutusData) {
        PlutusData property = mapPlutusData.getMap().get(BytesPlutusData.of(propertyName));
        if (property instanceof BytesPlutusData bytes) {
            return Optional.of(bytesToText(bytes.getValue()));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Reads a CIP-68 {@code uri} ({@code uri = bounded_bytes / [* bounded_bytes]}), used for the FT {@code logo}: a value
     * longer than 64 bytes, the most a Plutus byte string holds, is split into a list of byte-string chunks, and this
     * joins them back together. The chunks are joined as bytes and decoded once, so a multi-byte character cut by a
     * chunk boundary survives. Elements of the list that are not byte strings are ignored.
     * <p>
     * The value is capped at {@value #LOGO_MAX_BYTES} bytes (the CIP-26 logo has the same limit): an over-long value is
     * dropped with a warning and the rest of the datum is kept. The scheme is not validated; the value is stored as
     * written.
     */
    private Optional<String> getStringOrChunkedProperty(String propertyName, MapPlutusData mapPlutusData) {
        PlutusData property = mapPlutusData.getMap().get(BytesPlutusData.of(propertyName));

        byte[] value = switch (property) {
            case BytesPlutusData bytes -> bytes.getValue();
            case ListPlutusData list -> joinChunks(list);
            case null, default -> null;
        };
        if (value == null) {
            return Optional.empty();
        }
        if (value.length > LOGO_MAX_BYTES) {
            log.warn("Ignoring CIP-68 '{}' of {} bytes (max {})", propertyName, value.length, LOGO_MAX_BYTES);
            return Optional.empty();
        }
        // a single byte string keeps an empty value ("" is what some tokens declare); an empty list is nothing
        return property instanceof ListPlutusData && value.length == 0 ? Optional.empty() : Optional.of(bytesToText(value));
    }

    private static byte[] joinChunks(ListPlutusData list) {
        ByteArrayOutputStream joined = new ByteArrayOutputStream();
        for (PlutusData chunk : list.getPlutusDataList()) {
            if (chunk instanceof BytesPlutusData bytes) {
                joined.writeBytes(bytes.getValue());
            }
        }
        return joined.toByteArray();
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

    /**
     * Bytes as text the way CIP-68 says to convert metadata to JSON: UTF-8 when the bytes are valid UTF-8, hex
     * otherwise. Null characters are stripped from text.
     */
    private static String bytesToText(byte[] bytes) {
        return StringUtil.isValidUTF8(bytes)
                ? new String(bytes, StandardCharsets.UTF_8).replace("\0", "")
                : HexUtil.encodeHexString(bytes);
    }

}
