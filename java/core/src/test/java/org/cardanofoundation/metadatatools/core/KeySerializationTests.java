package org.cardanofoundation.metadatatools.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.java.Log;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.math.ec.rfc8032.Ed25519;
import org.bouncycastle.util.encoders.Hex;
import org.cardanofoundation.metadatatools.core.crypto.keys.Key;
import org.cardanofoundation.metadatatools.core.model.KeyTextEnvelope;
import org.cardanofoundation.metadatatools.core.model.KeyTextEnvelopeType;
import org.cardanofoundation.metadatatools.core.crypto.keys.KeyType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Arrays;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@Log
public class KeySerializationTests {

    private final Path RESOURCE_DIRECTORY = Paths.get("src", "test", "resources");

    @Test
    void Should_Succeed_When_SigningAndVerifyingWithShelleyEd25519Keys() throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();
        final KeyTextEnvelope verifyEnvelope = objectMapper.readValue(RESOURCE_DIRECTORY.resolve("payment.vkey").toFile().getAbsoluteFile(), KeyTextEnvelope.class);
        final KeyTextEnvelope signEnvelope = objectMapper.readValue(RESOURCE_DIRECTORY.resolve("payment.skey").toFile().getAbsoluteFile(), KeyTextEnvelope.class);
        final Key vkey = Key.fromTextEnvelope(verifyEnvelope);
        final Key skey = Key.fromTextEnvelope(signEnvelope);
        final String dataToSign = "Some bottles of wine move from Alice to Bob.";
        final byte[] signature = skey.sign(dataToSign.getBytes(StandardCharsets.UTF_8));
        assertThat(signature.length).isEqualTo(Ed25519.SIGNATURE_SIZE);
        final boolean verificationResult = vkey.verify(dataToSign.getBytes(StandardCharsets.UTF_8), signature);
        assertThat(verificationResult).isTrue();
        final boolean verificationResultFromSKey = skey.verify(dataToSign.getBytes(StandardCharsets.UTF_8), signature);
        assertThat(verificationResultFromSKey).isTrue();
    }

    @Test
    void Should_Succeed_When_SigningAndVerifyingWithRawEd25519Keys() {
        final Ed25519PrivateKeyParameters privateKeyParams = new Ed25519PrivateKeyParameters(new SecureRandom());
        final Ed25519PublicKeyParameters publicKeyParams = privateKeyParams.generatePublicKey();
        final Key vkey = Key.fromRaw(KeyType.POLICY_VERIFICATION_KEY_ED25519, publicKeyParams.getEncoded());
        final Key skey = Key.fromRaw(KeyType.POLICY_SIGNING_KEY_ED25519, privateKeyParams.getEncoded());
        assertThat(vkey.getChainCode()).isNull();
        assertThat(vkey.getKeyType()).isEqualTo(KeyType.POLICY_VERIFICATION_KEY_ED25519);
        assertThat(skey.getChainCode()).isNull();
        assertThat(skey.getKeyType()).isEqualTo(KeyType.POLICY_SIGNING_KEY_ED25519);
        final String dataToSign = "Some bottles of wine move from Alice to Bob.";
        final byte[] signature = skey.sign(dataToSign.getBytes(StandardCharsets.UTF_8));
        assertThat(signature.length).isEqualTo(Ed25519.SIGNATURE_SIZE);
        final boolean verificationResult = vkey.verify(dataToSign.getBytes(StandardCharsets.UTF_8), signature);
        assertThat(verificationResult).isTrue();
        final boolean verificationResultFromSKey = skey.verify(dataToSign.getBytes(StandardCharsets.UTF_8), signature);
        assertThat(verificationResultFromSKey).isTrue();
    }

    @Test
    void Should_Fail_When_SigningAndVerifyingWithRawEd25519KeysUsingWrongSignature() throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();
        final KeyTextEnvelope verifyEnvelope = objectMapper.readValue(RESOURCE_DIRECTORY.resolve("payment.vkey").toFile().getAbsoluteFile(), KeyTextEnvelope.class);
        final KeyTextEnvelope signEnvelope = objectMapper.readValue(RESOURCE_DIRECTORY.resolve("payment.skey").toFile().getAbsoluteFile(), KeyTextEnvelope.class);
        final Key vkey = Key.fromTextEnvelope(verifyEnvelope);
        final Key skey = Key.fromTextEnvelope(signEnvelope);
        final String dataToSign = "Some bottles of wine move from Alice to Bob.";
        final byte[] signature = skey.sign(dataToSign.getBytes(StandardCharsets.UTF_8));
        assertThat(signature.length).isEqualTo(Ed25519.SIGNATURE_SIZE);
        final byte[] wrongSignature = Arrays.copyOf(signature, signature.length);
        wrongSignature[0] ^= wrongSignature[0];

        final boolean verificationResult = vkey.verify(dataToSign.getBytes(StandardCharsets.UTF_8), wrongSignature);
        assertThat(verificationResult).isFalse();

        final byte[] signatureTooShort = Arrays.copyOf(signature, signature.length - 1);
        assertThatThrownBy(() -> vkey.verify(dataToSign.getBytes(StandardCharsets.UTF_8), signatureTooShort))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(String.format("Expected signature size of %d bytes but got %d.", Ed25519.SIGNATURE_SIZE, signatureTooShort.length));

        final boolean verificationResultFromSKey = skey.verify(dataToSign.getBytes(StandardCharsets.UTF_8), wrongSignature);
        assertThat(verificationResultFromSKey).isFalse();

        assertThatThrownBy(() -> skey.verify(dataToSign.getBytes(StandardCharsets.UTF_8), signatureTooShort))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(String.format("Expected signature size of %d bytes but got %d.", Ed25519.SIGNATURE_SIZE, signatureTooShort.length));
    }

    @Test
    void Should_Succeed_When_LoadingSimpleVerificationKeyFromTextEnvelopeWithoutExplicitKeyTypeParameter() throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();
        final KeyTextEnvelope envelope = objectMapper.readValue(RESOURCE_DIRECTORY.resolve("payment.vkey").toFile().getAbsoluteFile(), KeyTextEnvelope.class);
        final Key key = Key.fromTextEnvelope(envelope);
        final Key bech32DecodedKey = Key.fromBech32(key.toBech32());
        assertThat(envelope.getType()).isEqualTo(KeyTextEnvelopeType.PAYMENT_VERIFICATION_KEY_SHELLEY_ED25519);
        assertThat(key.getChainCode()).isNull();
        assertThat(key.getKeyType()).isEqualTo(KeyType.ACCOUNT_VERIFICATION_KEY_ED25519);
        assertThat(Hex.toHexString(key.getRawKeyBytes())).isEqualTo("500d9dad475bd8d12bfee5482f79ea502534563bf5cfd0044d72281498c05f0c");
        assertThat(key.getKeyType()).isEqualTo(bech32DecodedKey.getKeyType());
        assertThat(key.getRawKeyBytes()).isEqualTo(bech32DecodedKey.getRawKeyBytes());
        assertThat(key.getChainCode()).isEqualTo(bech32DecodedKey.getChainCode());
    }

    @Test
    void Should_Succeed_When_LoadingSimpleVerificationKeyFromTextEnvelopeWithExplicitKeyTypeParameter() throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();
        final KeyTextEnvelope envelope = objectMapper.readValue(RESOURCE_DIRECTORY.resolve("payment.vkey").toFile().getAbsoluteFile(), KeyTextEnvelope.class);
        final Key key = Key.fromTextEnvelope(envelope, KeyType.POLICY_VERIFICATION_KEY_ED25519);
        final Key bech32DecodedKey = Key.fromBech32(key.toBech32());
        assertThat(envelope.getType()).isEqualTo(KeyTextEnvelopeType.PAYMENT_VERIFICATION_KEY_SHELLEY_ED25519);
        assertThat(key.getChainCode()).isNull();
        assertThat(key.getKeyType()).isEqualTo(KeyType.POLICY_VERIFICATION_KEY_ED25519);
        assertThat(Hex.toHexString(key.getRawKeyBytes())).isEqualTo("500d9dad475bd8d12bfee5482f79ea502534563bf5cfd0044d72281498c05f0c");
        assertThat(key.getKeyType()).isEqualTo(bech32DecodedKey.getKeyType());
        assertThat(key.getRawKeyBytes()).isEqualTo(bech32DecodedKey.getRawKeyBytes());
        assertThat(key.getChainCode()).isEqualTo(bech32DecodedKey.getChainCode());
    }

    @Test
    void Should_ThrowIllegalArgumentException_When_LoadingSimpleVerificationKeyFromTextEnvelopeWithExplicitWrongKeyTypeParameter() throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();
        final KeyTextEnvelope envelope = objectMapper.readValue(RESOURCE_DIRECTORY.resolve("payment.vkey").toFile().getAbsoluteFile(), KeyTextEnvelope.class);
        assertThatThrownBy(() -> Key.fromTextEnvelope(envelope, KeyType.POLICY_SIGNING_KEY_ED25519))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Given key type features do not match inferred key type features.");
    }

    @Test
    void Should_Succeed_When_LoadingSimpleSigningKeyFromTextEnvelopeWithoutExplicitKeyTypeParameter() throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();
        final KeyTextEnvelope envelope = objectMapper.readValue(RESOURCE_DIRECTORY.resolve("payment.skey").toFile().getAbsoluteFile(), KeyTextEnvelope.class);
        final Key key = Key.fromTextEnvelope(envelope);
        final Key bech32DecodedKey = Key.fromBech32(key.toBech32());
        assertThat(envelope.getType()).isEqualTo(KeyTextEnvelopeType.PAYMENT_SIGNING_KEY_SHELLEY_ED25519);
        assertThat(key.getChainCode()).isNull();
        assertThat(key.getKeyType()).isEqualTo(KeyType.ACCOUNT_SIGNING_KEY_ED25519);
        assertThat(Hex.toHexString(key.getRawKeyBytes())).isEqualTo("29194775e85ced414b255ef6f7c4c64d5f2d9b89a21dcdf7d78827472225ccd8");
        assertThat(key.getKeyType()).isEqualTo(bech32DecodedKey.getKeyType());
        assertThat(key.getRawKeyBytes()).isEqualTo(bech32DecodedKey.getRawKeyBytes());
        assertThat(key.getChainCode()).isEqualTo(bech32DecodedKey.getChainCode());
    }

    @Test
    void Should_ThrowIllegalArgumentExecption_When_LoadingByronVerificationKeyFromTextEnvelope() throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();
        final KeyTextEnvelope envelope = objectMapper.readValue(RESOURCE_DIRECTORY.resolve("byron.vkey").toFile().getAbsoluteFile(), KeyTextEnvelope.class);
        assertThatThrownBy(() -> Key.fromTextEnvelope(envelope))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Byron keys are currently not supported.");
    }

    @Test
    void Should_ThrowIllegalArgumentExecption_When_LoadingByronSigningKeyFromTextEnvelope() throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();
        final KeyTextEnvelope envelope = objectMapper.readValue(RESOURCE_DIRECTORY.resolve("byron.skey").toFile().getAbsoluteFile(), KeyTextEnvelope.class);
        assertThatThrownBy(() -> Key.fromTextEnvelope(envelope))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Byron keys are currently not supported.");
    }

    @Test
    void Should_Succeed_When_LoadingBech32ExtendedRootSigningKey() {
        final String bech32String = "root_xsk1az0ggs8fhevvfhx2r5e52m9x7ck2hgprz2ggep3tckp6m5hrj3daqp9m9j6pyhe9800d8v025lfrg943utk35r608krvvgynf8kx43sank8l3j6ymqep2jk5z5terktgdqpglwd4ys5z853mlk8udnqhkgkn8uua";
        final Key key = Key.fromBech32(bech32String);
        final String reversedBech32String = key.toBech32();
        assertThat(key.getKeyType()).isEqualTo(KeyType.ROOT_EXTENDED_SIGNING_KEY_ED25519);
        assertThat(key.getChainCode()).isNotNull();
        assertThat(key.getChainCode().length).isEqualTo(Key.CHAIN_CODE_SIZE);
        assertThat(key.getRawKeyBytes().length).isEqualTo(Key.EXTENDED_SIGNING_KEY_SIZE);
        assertThat(reversedBech32String).isEqualTo(bech32String);
    }

    @Test
    void Should_Succeed_When_LoadingBech32AddressVerificationKey() {
        final String bech32String = "addr_vk1w0l2sr2zgfm26ztc6nl9xy8ghsk5sh6ldwemlpmp9xylzy4dtf7st80zhd";
        final Key key = Key.fromBech32(bech32String);
        final String reversedBech32String = key.toBech32();
        assertThat(key.getKeyType()).isEqualTo(KeyType.ADDRESS_VERIFICATION_KEY_ED25519);
        assertThat(key.getChainCode()).isNull();
        assertThat(key.getRawKeyBytes().length).isEqualTo(Ed25519.PUBLIC_KEY_SIZE);
        assertThat(reversedBech32String).isEqualTo(bech32String);
    }
}
