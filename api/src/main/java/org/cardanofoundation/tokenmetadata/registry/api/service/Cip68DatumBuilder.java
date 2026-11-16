package org.cardanofoundation.tokenmetadata.registry.api.service;

import com.bloxbean.cardano.client.common.cbor.CborSerializationUtil;
import com.bloxbean.cardano.client.plutus.spec.BigIntPlutusData;
import com.bloxbean.cardano.client.plutus.spec.BytesPlutusData;
import com.bloxbean.cardano.client.plutus.spec.ConstrPlutusData;
import com.bloxbean.cardano.client.plutus.spec.MapPlutusData;
import com.bloxbean.cardano.client.util.HexUtil;
import jakarta.annotation.Nullable;
import org.cardanofoundation.tokenmetadata.registry.api.model.validate.Cip68Fields;

import java.math.BigInteger;

/**
 * Builds the inline datum of a CIP-68 reference NFT from token metadata: {@code Constr 0 [metadata map, version]}, the
 * layout CIP-68 defines. Text values are UTF-8 byte strings (cardano-client-lib splits a byte string longer than 64
 * bytes into chunks, as the ledger requires), {@code decimals} is an integer. Fields that are not set are left out, so
 * that a missing mandatory field is reported, not invented.
 */
public final class Cip68DatumBuilder {

    /** The version a datum gets when the fields do not say: 1, the first version CIP-68 defines. */
    public static final long DEFAULT_VERSION = 1L;

    private Cip68DatumBuilder() {
    }

    /** The datum as CBOR hex, as it would be attached to the output that holds the reference NFT. */
    public static String build(Cip68Fields fields) {
        MapPlutusData metadata = new MapPlutusData();
        putText(metadata, Cip68FTDatumParser.NAME, fields.name());
        putText(metadata, Cip68FTDatumParser.DESCRIPTION, fields.description());
        putText(metadata, Cip68FTDatumParser.TICKER, fields.ticker());
        putText(metadata, Cip68FTDatumParser.URL, fields.url());
        putText(metadata, Cip68FTDatumParser.LOGO, fields.logo());
        putText(metadata, Cip68FTDatumParser.IMAGE, fields.image());
        putText(metadata, Cip68FTDatumParser.MEDIA_TYPE, fields.mediaType());
        if (fields.decimals() != null) {
            metadata.put(BytesPlutusData.of(Cip68FTDatumParser.DECIMALS), BigIntPlutusData.of(BigInteger.valueOf(fields.decimals())));
        }
        long version = fields.version() != null ? fields.version() : DEFAULT_VERSION;
        ConstrPlutusData datum = ConstrPlutusData.of(0, metadata, BigIntPlutusData.of(BigInteger.valueOf(version)));
        try {
            return HexUtil.encodeHexString(CborSerializationUtil.serialize(datum.serialize()));
        } catch (Exception e) {
            throw new IllegalArgumentException("the datum cannot be encoded: " + e.getMessage(), e);
        }
    }

    private static void putText(MapPlutusData metadata, String key, @Nullable String value) {
        if (value != null) {
            metadata.put(BytesPlutusData.of(key), BytesPlutusData.of(value));
        }
    }
}
