package org.cardanofoundation.metadatatools.core.crypto.keys;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import lombok.*;
import lombok.extern.log4j.Log4j2;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.math.ec.rfc8032.Ed25519;
import org.cardanofoundation.metadatatools.core.crypto.Bech32;
import org.cardanofoundation.metadatatools.core.model.KeyTextEnvelope;
import org.cardanofoundation.metadatatools.core.model.KeyTextEnvelopeType;

import java.io.IOException;
import java.util.Arrays;

/**
 * Represents cryptographic keys. Support raw binary format, text envelope (JSON) format and CIP-16 compliant key
 * representations based on bech32 encoding and CIP-5 compliant prefixes.
 */
@Log4j2
@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.PUBLIC)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
public class Key {
    public static final int CHAIN_CODE_SIZE = 32;
    public static final int EXTENDED_SIGNING_KEY_SIZE = 64;

    private final KeyType keyType;
    private final byte[] rawKeyBytes;   // the actual ED25519 key
    private final byte[] chainCode;     // the chain code associated with that key if any

    public byte[] sign(final byte[] data) {
        if (this.keyType.isSigningKey()) {
            final byte[] signature = new byte[Ed25519.SIGNATURE_SIZE];
            Ed25519.sign(this.rawKeyBytes, 0, data, 0, data.length, signature, 0);
            return signature;
        } else {
            throw new IllegalStateException("This is not a signing key.");
        }
    }

    public boolean verify(final byte[] data, final byte[] signature) {
        if (signature.length != Ed25519.SIGNATURE_SIZE) {
            throw new IllegalArgumentException(String.format("Expected signature size of %d bytes but got %d.", Ed25519.SIGNATURE_SIZE, signature.length));
        }

        if (this.keyType.isSigningKey()) {
            final Ed25519PrivateKeyParameters privateKeyParams = new Ed25519PrivateKeyParameters(Arrays.copyOfRange(this.rawKeyBytes, 0, Ed25519.SECRET_KEY_SIZE));
            final Ed25519PublicKeyParameters publicKey = privateKeyParams.generatePublicKey();
            return Ed25519.verify(signature, 0, publicKey.getEncoded(), 0, data, 0, data.length);
        } else {
            return Ed25519.verify(signature, 0, this.rawKeyBytes, 0, data, 0, data.length);
        }
    }

    private static byte[] rawKeyBytesFromCborData(final KeyType keyType, final byte[] cborDeserialized) throws IllegalArgumentException {
        if (keyType.isSigningKey()) {
            if (keyType == KeyType.KES_SIGNING_KEY_ED25519) {
                throw new IllegalArgumentException("KES signing keys are currently not supported.");
            }
            if (keyType.isExtendedKey()) {
                if (cborDeserialized.length >= EXTENDED_SIGNING_KEY_SIZE) {
                    return Arrays.copyOfRange(cborDeserialized, 0, EXTENDED_SIGNING_KEY_SIZE);
                } else {
                    throw new IllegalArgumentException(String.format("CBOR payload with too few bytes. Got %d bytes but expected %d bytes.", cborDeserialized.length, EXTENDED_SIGNING_KEY_SIZE));
                }
            } else {
                if (cborDeserialized.length >= Ed25519.SECRET_KEY_SIZE) {
                    return Arrays.copyOfRange(cborDeserialized, 0, Ed25519.SECRET_KEY_SIZE);
                } else {
                    throw new IllegalArgumentException(String.format("CBOR payload with too few bytes. Got %d bytes but expected %d bytes.", cborDeserialized.length, Ed25519.SECRET_KEY_SIZE));
                }
            }
        } else {
            if (cborDeserialized.length >= Ed25519.PUBLIC_KEY_SIZE) {
                return Arrays.copyOfRange(cborDeserialized, 0, Ed25519.PUBLIC_KEY_SIZE);
            } else {
                throw new IllegalArgumentException(String.format("CBOR payload with too few bytes. Got %d bytes but expected %d bytes.", cborDeserialized.length, Ed25519.PUBLIC_KEY_SIZE));
            }
        }
    }

    private static byte[] chainCodeFromCborData(final KeyType keyType, final byte[] cborDeserialized) {
        if (keyType.isExtendedKey()) {
            if (keyType.isSigningKey()) {
                if (keyType == KeyType.KES_SIGNING_KEY_ED25519) {
                    throw new IllegalArgumentException("KES signing keys are currently not supported.");
                }
                if (cborDeserialized.length == EXTENDED_SIGNING_KEY_SIZE + CHAIN_CODE_SIZE) {
                    return Arrays.copyOfRange(cborDeserialized, EXTENDED_SIGNING_KEY_SIZE, EXTENDED_SIGNING_KEY_SIZE + CHAIN_CODE_SIZE);
                } else {
                    throw new IllegalArgumentException(String.format("CBOR payload with wrong count of bytes. Got %d bytes but expected %d bytes.", cborDeserialized.length, EXTENDED_SIGNING_KEY_SIZE + CHAIN_CODE_SIZE));
                }
            } else {
                if (cborDeserialized.length == Ed25519.PUBLIC_KEY_SIZE + CHAIN_CODE_SIZE) {
                    return Arrays.copyOfRange(cborDeserialized, Ed25519.PUBLIC_KEY_SIZE, Ed25519.PUBLIC_KEY_SIZE + CHAIN_CODE_SIZE);
                } else {
                    throw new IllegalArgumentException(String.format("CBOR payload with wrong count of bytes. Got %d bytes but expected %d bytes.", cborDeserialized.length, Ed25519.PUBLIC_KEY_SIZE + CHAIN_CODE_SIZE));
                }
            }
        } else {
            return null;
        }
    }

    private static byte[] cborFromRawKey(final byte[] rawKeyBytes, final byte[] chainCode) {
        try {
            final ObjectMapper objectMapper = new ObjectMapper(new CBORFactory());
            if (chainCode != null && chainCode.length > 0) {
                final byte[] extendedKeyBytes = new byte[rawKeyBytes.length + chainCode.length];
                System.arraycopy(rawKeyBytes, 0, extendedKeyBytes, 0, rawKeyBytes.length);
                System.arraycopy(chainCode, 0, extendedKeyBytes, rawKeyBytes.length, chainCode.length);
                return objectMapper.writeValueAsBytes(extendedKeyBytes);
            } else {
                return objectMapper.writeValueAsBytes(rawKeyBytes);
            }
        } catch (final JsonProcessingException e) {
            log.error("Cannot serialize raw key representation to CBOR.", e);
            return null;
        }
    }

    public static Key fromBech32(final String bech32Repr) {
        final Bech32.Bech32Data bech32DecodingResult = Bech32.decode(bech32Repr);
        final KeyType keyType = KeyType.fromHrp(bech32DecodingResult.getHrp());
        final byte[] bech32Data = bech32DecodingResult.getData();
        if (keyType.isExtendedKey()) {
            return new Key(keyType,
                    Arrays.copyOfRange(bech32Data, 0,
                            (keyType.isSigningKey()) ? EXTENDED_SIGNING_KEY_SIZE : Ed25519.PUBLIC_KEY_SIZE),
                    Arrays.copyOfRange(bech32Data,
                            (keyType.isSigningKey()) ? EXTENDED_SIGNING_KEY_SIZE : Ed25519.PUBLIC_KEY_SIZE,
                            ((keyType.isSigningKey()) ? EXTENDED_SIGNING_KEY_SIZE : Ed25519.PUBLIC_KEY_SIZE) + CHAIN_CODE_SIZE));
        } else {
            return new Key(keyType,
                    Arrays.copyOfRange(bech32Data, 0, (keyType.isSigningKey()) ? Ed25519.SECRET_KEY_SIZE : Ed25519.PUBLIC_KEY_SIZE),
                    null);
        }
    }

    public static Key fromRaw(final KeyType keyType, final byte[] rawKeyBytes) {
        return Key.fromRaw(keyType, rawKeyBytes, null);
    }

    public static Key fromRaw(final KeyType keyType, final byte[] rawKeyBytes, final byte[] chainCode) {
        return new Key(keyType, rawKeyBytes, chainCode);
    }

    public static Key fromTextEnvelope(final KeyTextEnvelope keyTextEnvelope) {
        return fromTextEnvelope(keyTextEnvelope, keyTextEnvelope.getType().getDefaultKeyType());
    }

    public static Key fromTextEnvelope(final KeyTextEnvelope keyTextEnvelope, final KeyType keyType) {
        if (keyTextEnvelope.getType() == KeyTextEnvelopeType.PAYMENT_SIGNING_KEY_BYRON_ED25519_BIP32 ||
                keyTextEnvelope.getType() == KeyTextEnvelopeType.PAYMENT_VERIFICATION_KEY_BYRON_ED25519_BIP32) {
            throw new IllegalArgumentException("Byron keys are currently not supported.");
        }

        try {
            final KeyType envelopeKeyType = keyTextEnvelope.getType().getDefaultKeyType();
            if (envelopeKeyType.isSigningKey() == keyType.isSigningKey() && envelopeKeyType.isExtendedKey() == keyType.isExtendedKey()) {
                final ObjectMapper objectMapper = new ObjectMapper(new CBORFactory());
                final byte[] cborDeserialized = objectMapper.readValue(keyTextEnvelope.getCbor(), byte[].class);
                return new Key(keyType,
                        rawKeyBytesFromCborData(keyType, cborDeserialized),
                        chainCodeFromCborData(keyType, cborDeserialized));
            } else {
                throw new IllegalArgumentException("Given key type features do not match inferred key type features.");
            }
        } catch (final IOException e) {
            throw new IllegalArgumentException("Cannot deserialize key from given cbor representation.", e);
        }
    }

    final public String toBech32() {
        if (keyType.isExtendedKey()) {
            if (keyType.isSigningKey()) {
                if (this.rawKeyBytes.length == EXTENDED_SIGNING_KEY_SIZE && this.chainCode.length == CHAIN_CODE_SIZE) {
                    final byte[] concatenatedBytes = new byte[EXTENDED_SIGNING_KEY_SIZE + CHAIN_CODE_SIZE];
                    System.arraycopy(rawKeyBytes, 0, concatenatedBytes, 0, EXTENDED_SIGNING_KEY_SIZE);
                    System.arraycopy(chainCode, 0, concatenatedBytes, EXTENDED_SIGNING_KEY_SIZE, CHAIN_CODE_SIZE);
                    return Bech32.encode(Bech32.Encoding.BECH32, this.keyType.getHrp(), concatenatedBytes);
                } else {
                    throw new IllegalStateException("Key type and key bytes do not match.");
                }
            } else {
                if (this.rawKeyBytes.length == Ed25519.PUBLIC_KEY_SIZE && this.chainCode.length == CHAIN_CODE_SIZE) {
                    final byte[] concatenatedBytes = new byte[Ed25519.PUBLIC_KEY_SIZE + CHAIN_CODE_SIZE];
                    System.arraycopy(rawKeyBytes, 0, concatenatedBytes, 0, Ed25519.PUBLIC_KEY_SIZE);
                    System.arraycopy(chainCode, 0, concatenatedBytes, Ed25519.PUBLIC_KEY_SIZE, CHAIN_CODE_SIZE);
                    return Bech32.encode(Bech32.Encoding.BECH32, this.keyType.getHrp(), concatenatedBytes);
                } else {
                    throw new IllegalStateException("Key type and key bytes do not match.");
                }
            }
        } else {
            if ((keyType.isSigningKey() && this.rawKeyBytes.length == Ed25519.SECRET_KEY_SIZE) ||
                    (!keyType.isSigningKey() && this.rawKeyBytes.length == Ed25519.PUBLIC_KEY_SIZE)) {
                return Bech32.encode(Bech32.Encoding.BECH32, this.keyType.getHrp(), this.rawKeyBytes);
            } else {
                throw new IllegalStateException("Key type and key bytes do not match.");
            }
        }
    }

    final public KeyTextEnvelope toTextEnvelope(final KeyTextEnvelopeType keyTextEnvelopeType) {
        if (KeyTextEnvelopeType.isCompatibleWith(keyTextEnvelopeType, this.keyType)) {
            return new KeyTextEnvelope(keyTextEnvelopeType, cborFromRawKey(this.rawKeyBytes, this.chainCode));
        } else {
            throw new IllegalArgumentException("Given type of text envelope is not compatible with actual key type.");
        }
    }

    final public Key generateVerificationKey() {
        if (this.keyType.isSigningKey()) {
            final Ed25519PrivateKeyParameters privateKeyParams = new Ed25519PrivateKeyParameters(this.rawKeyBytes, 0);
            Ed25519PublicKeyParameters publicKeyParams = privateKeyParams.generatePublicKey();
            return new Key(KeyType.toVerificationKeyType(this.keyType), publicKeyParams.getEncoded(), this.chainCode);
        } else {
            return this;
        }
    }
}
