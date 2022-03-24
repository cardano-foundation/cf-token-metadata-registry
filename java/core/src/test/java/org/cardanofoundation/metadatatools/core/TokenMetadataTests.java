package org.cardanofoundation.metadatatools.core;

import com.bloxbean.cardano.client.exception.CborDeserializationException;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import lombok.extern.java.Log;
import org.bouncycastle.crypto.digests.Blake2bDigest;
import org.bouncycastle.util.encoders.Hex;
import org.cardanofoundation.metadatatools.core.crypto.keys.Key;
import org.cardanofoundation.metadatatools.core.crypto.keys.KeyType;
import org.cardanofoundation.metadatatools.core.model.KeyTextEnvelope;
import org.cardanofoundation.metadatatools.core.model.PolicyScript;
import org.cardanofoundation.metadatatools.core.model.TokenMetadataProperty;
import org.cardanofoundation.metadatatools.core.model.TokenMetadata;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


@Log
public class TokenMetadataTests {

    private final Path RESOURCE_DIRECTORY = Paths.get("src", "test", "resources");

    @Test
    public void Should_Succeed_When_SerializingTokenMetadata() throws IOException {
        //      hash(
        //          hash(0x75613534303864306462306439343266643830333734) +  // text ""
        //          hash(0x67636F6E74616374) +                              // text "contact"
        //          hash(0x6A436964204B72616D6572) +                        // text "Cid Kramer"
        //          hash(0x00)                                              // uint 0
        //      )
        //      value to sign: cd731afcc904c521e0c6b3cc0b560b8157ee29c3e41cd15f8dc8984edf600029
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        final KeyTextEnvelope verifyEnvelope = objectMapper.readValue(RESOURCE_DIRECTORY.resolve("payment.vkey").toFile().getAbsoluteFile(), KeyTextEnvelope.class);
        final KeyTextEnvelope signEnvelope = objectMapper.readValue(RESOURCE_DIRECTORY.resolve("payment.skey").toFile().getAbsoluteFile(), KeyTextEnvelope.class);
        final Key vkey = Key.fromTextEnvelope(verifyEnvelope);
        final Key skey = Key.fromTextEnvelope(signEnvelope);

        final ObjectMapper cborMapper = new ObjectMapper(new CBORFactory());
        final String policyJson = "{\n" +
                "  \"type\": \"atLeast\",\n" +
                "  \"required\": 2,\n" +
                "  \"scripts\":\n" +
                "  [\n" +
                "    {\n" +
                "      \"type\": \"before\",\n" +
                "      \"slot\": 600\n" +
                "    },\n" +
                "    {\n" +
                "      \"type\": \"sig\",\n" +
                "      \"keyHash\": \"fb864e59bf8620349c3ebe29af5ad0f9ca2e319d39e115eb93aa58a4\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"type\": \"after\",\n" +
                "      \"slot\": 500\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        final String simplePolicyJson = "{\n" +
                "  \"type\": \"sig\",\n" +
                "  \"keyHash\": \"fb864e59bf8620349c3ebe29af5ad0f9ca2e319d39e115eb93aa58a4\"\n" +
                "}";
        final PolicyScript simpleJsonScript = objectMapper.readValue(simplePolicyJson, PolicyScript.class);
        final String simpleJsonScriptSerializedExpected = objectMapper.writeValueAsString(simpleJsonScript);
        final String simplePolicyCborSerializedExpected = "82008200581cfb864e59bf8620349c3ebe29af5ad0f9ca2e319d39e115eb93aa58a4";
        final byte[] simplePolicyCborSerialized = cborMapper.writeValueAsBytes(simpleJsonScript.toCborTree(cborMapper));
        assertThat(Hex.toHexString(simplePolicyCborSerialized)).isEqualTo(simplePolicyCborSerializedExpected);
        final PolicyScript simpleCborScriptDeserialized = PolicyScript.fromCborTree(cborMapper.readTree(simplePolicyCborSerialized));
        final String simpleCborScriptDeserializedJsonSerialized = objectMapper.writeValueAsString(simpleCborScriptDeserialized);
        assertThat(simpleCborScriptDeserializedJsonSerialized).isEqualTo(simpleJsonScriptSerializedExpected);

        final PolicyScript jsonScript = objectMapper.readValue(policyJson, PolicyScript.class);
        final String jsonScriptSerializedExpected = objectMapper.writeValueAsString(jsonScript);
        final String policyCborSerializedExpected = "82018303028382051902588200581cfb864e59bf8620349c3ebe29af5ad0f9ca2e319d39e115eb93aa58a482041901f4";
        final byte[] policyCborSerialized = cborMapper.writeValueAsBytes(jsonScript.toCborTree(cborMapper));
        assertThat(Hex.toHexString(policyCborSerialized)).isEqualTo(policyCborSerializedExpected);
        final PolicyScript cborScriptDeserialized = PolicyScript.fromCborTree(cborMapper.readTree(policyCborSerialized));
        final String cborScriptDeserializedJsonSerialized = objectMapper.writeValueAsString(cborScriptDeserialized);
        assertThat(cborScriptDeserializedJsonSerialized).isEqualTo(jsonScriptSerializedExpected);

        final String vkeyhex = "04a72e68bd7601aa1cc1da7194676b0f8c9fb55be0291f1089ff5d6ce5e2998a";
        final byte[] vkeyraw = Hex.decode(vkeyhex);
        final Blake2bDigest b2b224 = new Blake2bDigest(224);
        b2b224.update(vkeyraw, 0, vkeyraw.length);
        int digestSize = b2b224.getDigestSize();
        final byte[] vkeyb2braw = new byte[digestSize];
        b2b224.doFinal(vkeyb2braw, 0);
        final String vkeyb2bhex = Hex.toHexString(vkeyb2braw);

        final TokenMetadata tokenMetadata = new TokenMetadata();
        tokenMetadata.setSubject("3f36f6048afc298de6254c6a7ecc0f5517cdec72875bb73c3dc5795a414d414e544953");
        tokenMetadata.setPolicy("820182018282051a0337a9668200581c2072220a7b83b90ae2b17e5f61d825e4cccc5eb39646a452f3b48ac6");
        tokenMetadata.addProperty("name", new TokenMetadataProperty<>("MelMcCoin", 0, null));
        tokenMetadata.addProperty("description", new TokenMetadataProperty<>("We test with MelMcCoin.", 0, null));
        tokenMetadata.addProperty("ticker", new TokenMetadataProperty<>("MelMcCoin.", 0, null));
        tokenMetadata.addProperty("decimals", new TokenMetadataProperty<>(0, 0, null));
        TokenMetadataCreator.signTokenMetadata(tokenMetadata, skey);

        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        String jsonStrRepr = objectMapper.writeValueAsString(tokenMetadata);
        log.info(jsonStrRepr);

        TokenMetadataCreator.signTokenMetadata(tokenMetadata, skey, "contact");
        jsonStrRepr = objectMapper.writeValueAsString(tokenMetadata);
        log.info(jsonStrRepr);

        TokenMetadataCreator.validateTokenMetadata(tokenMetadata, vkey, false);
    }

    @Test
    public void Should_Succeed_When_SigningSingleWitnessReferenceData() throws IOException, CborDeserializationException, CborSerializationException {
        final String assetName = "myassetname";
        final ObjectMapper jsonMapper = new ObjectMapper();
        final KeyTextEnvelope signingEnvelope = jsonMapper.readValue(RESOURCE_DIRECTORY.resolve("policy.skey").toFile().getAbsoluteFile(), KeyTextEnvelope.class);
        final Key signingKey = Key.fromTextEnvelope(signingEnvelope, KeyType.POLICY_SIGNING_KEY_ED25519);
        final Key verificationKey = signingKey.generateVerificationKey();
        final PolicyScript policyScript = jsonMapper.readValue(RESOURCE_DIRECTORY.resolve("policy.script").toFile().getAbsoluteFile(), PolicyScript.class);
        final TokenMetadata tokenMetadata = new TokenMetadata(assetName, policyScript);
        tokenMetadata.addProperty("name", new TokenMetadataProperty<>("MelMcCoin", 0, null));
        tokenMetadata.addProperty("description", new TokenMetadataProperty<>("We test with MelMcCoin", 0, null));
        TokenMetadataCreator.signTokenMetadata(tokenMetadata, signingKey);
        final ValidationResult validationResult = TokenMetadataCreator.validateTokenMetadata(tokenMetadata, verificationKey);
        assertThat(validationResult.isValid()).isTrue();
        assertThat(validationResult.getValidationErrors().size()).isEqualTo(0);
        assertThat(tokenMetadata.getSubject()).isEqualTo("6ad121cd218e513bdb8ad67afc04d188f859b25d258a694c38269941" + Hex.toHexString(assetName.getBytes(StandardCharsets.UTF_8)));
        assertThat(tokenMetadata.getPolicy()).isEqualTo("82008200581cfb864e59bf8620349c3ebe29af5ad0f9ca2e319d39e115eb93aa58a4");
        assertThat(tokenMetadata.getTool()).isNull();
        assertThat(tokenMetadata.getProperties().get("name").getSequenceNumber()).isEqualTo(0);
        assertThat(tokenMetadata.getProperties().get("name").getValue()).isEqualTo("MelMcCoin");
        assertThat(tokenMetadata.getProperties().get("name").getSignatures().size()).isEqualTo(1);
        assertThat(tokenMetadata.getProperties().get("name").getSignatures().get(0).getPublicKey()).isEqualTo(Hex.toHexString(verificationKey.getRawKeyBytes()));
        assertThat(tokenMetadata.getProperties().get("name").getSignatures().get(0).getSignature()).isEqualTo("cd536430044140c36f4987e2f58f3772340af3b8865b0100838065d95b1a439abfe235bc55b52b5894f03c6a008bd313f8579fda410b020bb673c426e80cf602");
        assertThat(tokenMetadata.getProperties().get("description").getSequenceNumber()).isEqualTo(0);
        assertThat(tokenMetadata.getProperties().get("description").getValue()).isEqualTo("We test with MelMcCoin");
        assertThat(tokenMetadata.getProperties().get("description").getSignatures().size()).isEqualTo(1);
        assertThat(tokenMetadata.getProperties().get("description").getSignatures().get(0).getPublicKey()).isEqualTo(Hex.toHexString(verificationKey.getRawKeyBytes()));
        assertThat(tokenMetadata.getProperties().get("description").getSignatures().get(0).getSignature()).isEqualTo("f94ea3ee685c1338c806089f33e83b7de50b953d02bd98d0051209e4cf3f42b3c057ea2748d13ae73110cd7220ccbf80bff5ef2eb90784378bf5537b36d1c207");
    }

    @Test
    public void Should_Succeed_When_SigningMultiWitnessReferenceData() throws IOException, CborDeserializationException, CborSerializationException {
        final String assetName = "myassetname";
        final ObjectMapper jsonMapper = new ObjectMapper();
        final KeyTextEnvelope signingEnvelope = jsonMapper.readValue(RESOURCE_DIRECTORY.resolve("policy.skey").toFile().getAbsoluteFile(), KeyTextEnvelope.class);
        final Key signingKey = Key.fromTextEnvelope(signingEnvelope, KeyType.POLICY_SIGNING_KEY_ED25519);
        final Key verificationKey = signingKey.generateVerificationKey();
        final PolicyScript policyScript = jsonMapper.readValue(RESOURCE_DIRECTORY.resolve("atLeastPolicy.script").toFile().getAbsoluteFile(), PolicyScript.class);
        final TokenMetadata tokenMetadata = new TokenMetadata(assetName, policyScript);
        tokenMetadata.addProperty("name", new TokenMetadataProperty<>("MelMcCoin", 0, null));
        tokenMetadata.addProperty("description", new TokenMetadataProperty<>("We test with MelMcCoin", 0, null));
        TokenMetadataCreator.signTokenMetadata(tokenMetadata, signingKey);
        final ValidationResult validationResult = TokenMetadataCreator.validateTokenMetadata(tokenMetadata, verificationKey);
        assertThat(validationResult.isValid()).isTrue();
        assertThat(validationResult.getValidationErrors().size()).isEqualTo(0);
        assertThat(tokenMetadata.getSubject()).isEqualTo("b0537110d01bbf847e48edf448d0f411d121e69ec31be256f46b1096" + Hex.toHexString(assetName.getBytes(StandardCharsets.UTF_8)));
        assertThat(tokenMetadata.getPolicy()).isEqualTo("82018303018382051902588200581cfb864e59bf8620349c3ebe29af5ad0f9ca2e319d39e115eb93aa58a482041901f4");
        assertThat(tokenMetadata.getTool()).isNull();
        assertThat(tokenMetadata.getProperties().get("name").getSequenceNumber()).isEqualTo(0);
        assertThat(tokenMetadata.getProperties().get("name").getValue()).isEqualTo("MelMcCoin");
        assertThat(tokenMetadata.getProperties().get("name").getSignatures().size()).isEqualTo(1);
        assertThat(tokenMetadata.getProperties().get("name").getSignatures().get(0).getPublicKey()).isEqualTo(Hex.toHexString(verificationKey.getRawKeyBytes()));
        assertThat(tokenMetadata.getProperties().get("name").getSignatures().get(0).getSignature()).isEqualTo("65f132beb84e8a5447172f27f787a8f88e855b218af0cb8d1b2373b08e32cd577abf063790d260835d5359e98e00f8e2c1d27b644e4ed3aadefb54815c6c990e");
        assertThat(tokenMetadata.getProperties().get("description").getSequenceNumber()).isEqualTo(0);
        assertThat(tokenMetadata.getProperties().get("description").getValue()).isEqualTo("We test with MelMcCoin");
        assertThat(tokenMetadata.getProperties().get("description").getSignatures().size()).isEqualTo(1);
        assertThat(tokenMetadata.getProperties().get("description").getSignatures().get(0).getPublicKey()).isEqualTo(Hex.toHexString(verificationKey.getRawKeyBytes()));
        assertThat(tokenMetadata.getProperties().get("description").getSignatures().get(0).getSignature()).isEqualTo("174d4225bb8a82f7def7dd49656d7d80abea298a2501338c9f9a94c577e131e7eb37fd0a9d6ebf556a0fe74f7f4533c4534c235edafc039131277a99b0e34b07");
    }
}
