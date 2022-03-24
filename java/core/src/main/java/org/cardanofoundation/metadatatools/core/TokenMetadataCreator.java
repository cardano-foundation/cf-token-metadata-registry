package org.cardanofoundation.metadatatools.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import lombok.extern.log4j.Log4j2;
import org.bouncycastle.jcajce.provider.digest.Blake2b;
import org.bouncycastle.math.ec.rfc8032.Ed25519;
import org.bouncycastle.util.encoders.Hex;
import org.cardanofoundation.metadatatools.core.crypto.keys.Key;
import org.cardanofoundation.metadatatools.core.model.AttestationSignature;
import org.cardanofoundation.metadatatools.core.model.PolicyScript;
import org.cardanofoundation.metadatatools.core.model.TokenMetadataProperty;
import org.cardanofoundation.metadatatools.core.model.TokenMetadata;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Log4j2
public class TokenMetadataCreator {
    public static ValidationResult validateTokenMetadata(final TokenMetadata metadata, final Key verificationKey) {
        return validateTokenMetadata(metadata, verificationKey, false);
    }

    public static ValidationResult validateTokenMetadata(final TokenMetadata metadata, final Key verificationKey, final boolean signaturesOnly) {
        if (metadata == null) {
            throw new IllegalArgumentException("metadata cannot be null.");
        }
        if (verificationKey == null) {
            throw new IllegalArgumentException("verificationKey cannot be null.");
        }
        if (verificationKey.getKeyType().isSigningKey()) {
            throw new IllegalArgumentException("This function expects a verification key. Public key derivation from private keys shall be done in client.");
        }

        final ValidationResult validationResult = new ValidationResult();
        try {
            final ObjectMapper cborMapper = new ObjectMapper(new CBORFactory());
            final PolicyScript policyScript = PolicyScript.fromCborTree(cborMapper.readTree(Hex.decode(metadata.getPolicy())));
            TokenMetadataValidationRules.validateSubjectAndPolicy(metadata.getSubject(), policyScript.computePolicyId(), validationResult);
        } catch (final IOException e) {
            validationResult.addValidationError("Could not deserialize policy script from policy value due to " + e.getMessage());
        }
        TokenMetadataValidationRules.validateHasRequiredProperties(metadata.getProperties().keySet(), validationResult);

        try {
            for (final Map.Entry<String, TokenMetadataProperty<?>> entry : metadata.getProperties().entrySet()) {
                if (!signaturesOnly) {
                    validationResult.mergeWith(TokenMetadataValidationRules.validateProperty(entry.getKey(), entry.getValue()));
                }
                for (final AttestationSignature attestationSignature : entry.getValue().getSignatures()) {
                    final ObjectMapper objectMapper = new ObjectMapper(new CBORFactory());
                    final Blake2b.Blake2b256 blake2b = new Blake2b.Blake2b256();
                    final byte[] subjectHash = blake2b.digest(objectMapper.writeValueAsBytes(metadata.getSubject()));
                    final byte[] propertyNameHash = blake2b.digest(objectMapper.writeValueAsBytes(entry.getKey()));
                    final byte[] valueHash = blake2b.digest(objectMapper.writeValueAsBytes(entry.getValue().getValue()));
                    final byte[] sequenceNumberHash = blake2b.digest(objectMapper.writeValueAsBytes(entry.getValue().getSequenceNumber()));
                    blake2b.update(subjectHash);
                    blake2b.update(propertyNameHash);
                    blake2b.update(valueHash);
                    blake2b.update(sequenceNumberHash);
                    final byte[] propertyHash = blake2b.digest();
                    final byte[] signatureRaw = Hex.decode(attestationSignature.getSignature());
                    final boolean result = Ed25519.verify(signatureRaw, 0, verificationKey.getRawKeyBytes(), 0, propertyHash, 0, propertyHash.length);
                    if (!result) {
                        validationResult.addValidationError(String.format("property %s: signature verification failed for key %s.", entry.getKey(), attestationSignature.getPublicKey()));
                    }
                }
            }
        } catch (final IOException e) {
            validationResult.addValidationError("Could not verify due to an internal error: " + e.getMessage());
        }

        return validationResult;
    }

    public static ValidationResult validateTokenMetadataUpdate(final TokenMetadata latest, final Key verificationKey, final TokenMetadata base) {
        final ValidationResult resultForLatest = validateTokenMetadata(latest, verificationKey);
        final ValidationResult resultForBase = validateTokenMetadata(base, verificationKey);
        if (resultForLatest.isValid() && resultForBase.isValid()) {
            final ValidationResult validationResult = new ValidationResult();
            //if ()
            //820182018282051a03347d8d8200581c39a1df51147b6de6689a4727846962fb6540c3a3c7859a1a79b9420f
            //820182018282051a033eb1be8200581c39a1df51147b6de6689a4727846962fb6540c3a3c7859a1a79b9420f
            // compare base to latest: check sequence numbers
            // compare base to latest: if base has signatures and latest not --> this is an error
            return validationResult;
        } else {
            return ValidationResult.mergeResults(List.of(resultForBase, resultForLatest));
        }
    }

    private static void signMetadataProperty(final TokenMetadataProperty<?> property, final Key signingKey, final Key verificationKey, final byte[] subjectHash, final byte[] propertyNameHash) {
        if (property == null) {
            throw new IllegalArgumentException("property cannot be null.");
        }
        if (signingKey == null) {
            throw new IllegalArgumentException("signing key cannot be null.");
        }
        if (!signingKey.getKeyType().isSigningKey()) {
            throw new IllegalArgumentException("Given signing key is no signing key.");
        }
        if (verificationKey == null) {
            throw new IllegalArgumentException("verification key cannot be null.");
        }
        if (verificationKey.getKeyType().isSigningKey()) {
            throw new IllegalArgumentException("Given verification key is no verification key.");
        }
        if (property.getValue() == null) {
            throw new IllegalArgumentException("property value cannot be null.");
        }
        if (property.getSequenceNumber() == null || property.getSequenceNumber() < 0) {
            throw new IllegalArgumentException("property sequenceNumber cannot be null or less than zero.");
        }
        if (subjectHash.length == 0) {
            log.warn("subject hash should not be empty");
        }
        if (propertyNameHash.length == 0) {
            log.warn("subject hash should not be empty");
        }

        try {
            final ObjectMapper objectMapper = new ObjectMapper(new CBORFactory());
            final byte[] cborValue = objectMapper.writeValueAsBytes(property.getValue());
            final byte[] cborSequenceNumber = objectMapper.writeValueAsBytes(property.getSequenceNumber());

            final Blake2b.Blake2b256 blake2b = new Blake2b.Blake2b256();
            final byte[] cborValueHash = blake2b.digest(cborValue);
            final byte[] cborSequenceNumberHash = blake2b.digest(cborSequenceNumber);
            blake2b.update(subjectHash);
            blake2b.update(propertyNameHash);
            blake2b.update(cborValueHash);
            blake2b.update(cborSequenceNumberHash);
            final byte[] propertyHash = blake2b.digest();
            final byte[] signature = signingKey.sign(propertyHash);
            property.addOrUpdateSignature(Hex.toHexString(verificationKey.getRawKeyBytes()), Hex.toHexString(signature));
        } catch (final IOException e) {
            throw new IllegalArgumentException("Cannot serialize property fields into cbor.", e);
        }
    }

    public static void signTokenMetadata(final TokenMetadata input, final Key signingKey, final String propertyName) {
        if (input == null) {
            throw new IllegalArgumentException("TokenMetadata object cannot be null.");
        }
        if (signingKey == null) {
            throw new IllegalArgumentException("Signing key cannot be null.");
        }
        if (!signingKey.getKeyType().isSigningKey()) {
            throw new IllegalArgumentException("Given key cannot be used for signing.");
        }
        if (propertyName == null) {
            throw new IllegalArgumentException("propertyName cannot be null.");
        }
        final String propertyNameSanitized = TokenMetadata.sanitizePropertyName(propertyName);
        if (propertyNameSanitized.isEmpty()) {
            throw new IllegalArgumentException("propertyName cannot be empty or blank.");
        }

        final TokenMetadataProperty<?> metadataProperty = input.getProperties().getOrDefault(propertyNameSanitized, null);
        if (metadataProperty != null) {
            try {
                final Key verificationKey = signingKey.generateVerificationKey();
                final Blake2b.Blake2b256 blake2b = new Blake2b.Blake2b256();
                final ObjectMapper objectMapper = new ObjectMapper(new CBORFactory());
                final byte[] cborSubject = objectMapper.writeValueAsBytes(input.getSubject());
                final byte[] subjectHash = blake2b.digest(cborSubject);
                final byte[] cborPropertyName = objectMapper.writeValueAsBytes(propertyName);
                final byte[] propertyNameHash = blake2b.digest(cborPropertyName);
                signMetadataProperty(metadataProperty, signingKey, verificationKey, subjectHash, propertyNameHash);
            } catch (final JsonProcessingException e) {
                throw new IllegalArgumentException("Cannot encode subject into cbor.", e);
            }
        }
    }

    public static void signTokenMetadata(final TokenMetadata input, final Key signingKey) {
        if (input == null) {
            throw new IllegalArgumentException("TokenMetadata object cannot be null.");
        }
        if (signingKey == null) {
            throw new IllegalArgumentException("Signing key cannot be null.");
        }
        if (!signingKey.getKeyType().isSigningKey()) {
            throw new IllegalArgumentException("Given key cannot be used for signing.");
        }

        for (final String property : input.getProperties().keySet()) {
            signTokenMetadata(input, signingKey, property);
        }
    }
}
