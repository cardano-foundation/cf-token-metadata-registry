package org.cardanofoundation.metadatatools.core.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.*;
import org.bouncycastle.util.encoders.Hex;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.PUBLIC)
@ToString
public class TokenMetadata {
    private static final List<String> REQUIRED_PROPERTIES = List.of("name", "description");

    private String subject;
    private String policy;
    private Map<String, TokenMetadataProperty<?>> properties;

    public TokenMetadata() {
        this.properties = new HashMap<>();
    }

    public TokenMetadata(final String assetName) throws IOException {
        this.properties = new HashMap<>();
        init(assetName);
    }

    public TokenMetadata(final String assetName, final PolicyScript policyScript) throws IOException {
        this.properties = new HashMap<>();
        init(assetName, policyScript);
    }

    public void setSubjectFromAssetNameAndPolicyId(final String assetName, final String policyId) {
        this.subject = policyId + Hex.toHexString(assetName.getBytes(StandardCharsets.UTF_8));
    }

    public void setSubjectFromAssetName(final String assetName) {
        this.subject = Hex.toHexString(assetName.getBytes(StandardCharsets.UTF_8));
    }

    public void init(final String assetName, final PolicyScript policyScript) throws IOException {
        final String policyId;
        if (policyScript != null) {
            policyId = policyScript.computePolicyId();
            this.policy = Hex.toHexString(policyScript.toCbor());
        } else {
            policyId = "";
        }
        this.subject = policyId + Hex.toHexString(assetName.getBytes(StandardCharsets.UTF_8));
    }

    public void init(final String assetName) throws IOException {
        init(assetName, null);
    }

    @JsonAnySetter
    public void setRequiredProperties(final Map<String, TokenMetadataProperty<?>> properties) {
        for (final Map.Entry<String, TokenMetadataProperty<?>> entry : properties.entrySet()) {
            addProperty(entry.getKey(), entry.getValue());
        }
    }

    @JsonAnyGetter
    public Map<String, TokenMetadataProperty<?>> getProperties() {
        return this.properties;
    }

    public void addProperty(final String propertyName, final TokenMetadataProperty<?> property) {
        if (propertyName == null) {
            throw new IllegalArgumentException("propertyName cannot be null.");
        }

        final String propertyNameSanitized = propertyName.toLowerCase(Locale.ROOT).trim();
        if (propertyNameSanitized.isEmpty()) {
            throw new IllegalArgumentException("propertyName cannot be empty or blank.");
        }

        if (property != null) {
            this.properties.put(propertyNameSanitized, property);
        } else {
            this.properties.remove(propertyNameSanitized);
        }
    }

    public void removeProperty(final String propertyName) {
        if (propertyName == null) {
            throw new IllegalArgumentException("propertyName cannot be null");
        }

        final String propertyNameSanitized = propertyName.trim().toLowerCase(Locale.ROOT);
        if (propertyNameSanitized.isEmpty()) {
            throw new IllegalArgumentException("propertyName cannot be empty or blank");
        }

        this.properties.remove(propertyNameSanitized);
    }

    @JsonRawValue
    private String tool;

    public static String sanitizePropertyName(final String propertyName) {
        if (propertyName == null) {
            throw new IllegalArgumentException("propertyName cannot be null.");
        }
        return propertyName.toLowerCase(Locale.ROOT).trim();
    }
}
