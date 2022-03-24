package org.cardanofoundation.metadatatools.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bouncycastle.crypto.digests.Blake2bDigest;
import org.bouncycastle.jcajce.provider.digest.Blake2b;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This implements besides the Json representation, the array based data model how monetary policy JSON scripts are
 * represented specified by the Alonzo CDDL.
 *
 * native_script =
 *   [ script_pubkey
 *   // script_all
 *   // script_any
 *   // script_n_of_k
 *   // invalid_before
 *      ; Timelock validity intervals are half-open intervals [a, b).
 *      ; This field specifies the left (included) endpoint a.
 *   // invalid_hereafter
 *      ; Timelock validity intervals are half-open intervals [a, b).
 *      ; This field specifies the right (excluded) endpoint b.
 *   ]
 *
 * script_pubkey = (0, addr_keyhash)
 * script_all = (1, [ * native_script ])
 * script_any = (2, [ * native_script ])
 * script_n_of_k = (3, n: uint, [ * native_script ])
 * invalid_before = (4, uint)
 * invalid_hereafter = (5, uint)
 */
@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.PUBLIC)
@NoArgsConstructor
@JsonPropertyOrder({ "scripts", "slot", "keyHash", "required", "type" })
public class PolicyScript {
    private enum ScriptLanguageNamespace {
        NATIVE_SCRIPT((byte) 0);

        private final byte namespaceId;

        ScriptLanguageNamespace(final byte namespaceId) {
            this.namespaceId = namespaceId;
        }

        final byte getNamespaceId() {
            return this.namespaceId;
        }
    }

    @Getter
    private enum NativeScriptType {
        @JsonProperty("sig")
        SCRIPT_PUBKEY("sig", 0),
        @JsonProperty("all")
        SCRIPT_ALL("all", 1),
        @JsonProperty("any")
        SCRIPT_ANY("any", 2),
        @JsonProperty("atLeast")
        SCRIPT_N_OF_K("atLeast", 3),
        @JsonProperty("after")
        INVALID_BEFORE("after", 4),
        @JsonProperty("before")
        INVALID_HEREAFTER("before", 5),
        @JsonProperty("required")
        REQUIRED("required", 0);

        private final String jsonLabel;
        private final int cborLabel;

        NativeScriptType(final String jsonLabel, final int cborLabel) {
            this.jsonLabel = jsonLabel;
            this.cborLabel = cborLabel;
        }

        public static NativeScriptType fromCborLabel(final int cborLabel) {
            for (final NativeScriptType type : NativeScriptType.values()) {
                if (type.cborLabel == cborLabel) {
                    return type;
                }
            }
            throw new IllegalArgumentException(String.format("Given cbor label %d is not supported.", cborLabel));
        }

        public static NativeScriptType fromJsonLabel(final String jsonLabel) {
            for (final NativeScriptType type : NativeScriptType.values()) {
                if (type.jsonLabel.equals(jsonLabel)) {
                    return type;
                }
            }
            throw new IllegalArgumentException(String.format("Given json label %s is not supported.", jsonLabel));
        }
    }

    private NativeScriptType type;
    private List<PolicyScript> scripts;
    private Integer slot;
    private Integer required;
    private String keyHash;

    private JsonNode toChildCborTree(final ObjectMapper objectMapper) {
        final ArrayNode node = objectMapper.createArrayNode();
        if (type != null) {
            node.add(type.cborLabel);
        }
        if (slot != null) {
            node.add(slot);
        }
        if (required != null) {
            node.add(required);
        }
        if (keyHash != null) {
            node.add(Hex.decode(keyHash));
        }
        return node;
    }

    public JsonNode toCborTree(final ObjectMapper objectMapper) {
        final ArrayNode root = objectMapper.createArrayNode();
        if (scripts == null || scripts.isEmpty()) {
            root.add(0);
            root.add(this.toChildCborTree(objectMapper));
        } else {
            root.add(1);
            final ArrayNode head = objectMapper.createArrayNode();
            head.add(this.type.cborLabel);
            if (required != null) {
                head.add(this.required);
            }
            final ArrayNode scriptsArrayNode = objectMapper.createArrayNode();
            for (final PolicyScript childScript : scripts) {
                scriptsArrayNode.add(childScript.toChildCborTree(objectMapper));
            }
            head.add(scriptsArrayNode);
            root.add(head);
        }
        return root;
    }

    public static String computePolicyId(final File file) throws IOException {
        final String content = Files.readString(file.toPath());
        return computePolicyId(content);
    }

    public static String computePolicyId(final String content) throws IOException {
        try {
            final ObjectMapper jsonMapper = new ObjectMapper();
            final PolicyScript policyScript = jsonMapper.readValue(content, PolicyScript.class);
            return policyScript.computePolicyId();
        } catch (final JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot compute policy id.", e);
        }
    }

    public final String computePolicyId() throws IOException {
        try {
            final byte[] scriptAsCbor = toCbor();
            final byte[] scriptNamespace = new byte[]{ScriptLanguageNamespace.NATIVE_SCRIPT.getNamespaceId()};
            final ByteArrayOutputStream boas = new ByteArrayOutputStream();
            boas.write(scriptNamespace);
            boas.write(scriptAsCbor, 2, scriptAsCbor.length - 2);
            final byte[] scriptSerializedWithNamespace = boas.toByteArray();
            final Blake2bDigest b2b224 = new Blake2bDigest(224);
            b2b224.update(scriptSerializedWithNamespace, 0, scriptSerializedWithNamespace.length);
            byte[] digests = new byte[b2b224.getDigestSize()];
            b2b224.doFinal(digests, 0);
            return Hex.toHexString(digests);
        } catch (final JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot compute policy id.", e);
        }
    }

    public final byte[] toCbor() {
        try {
            final ObjectMapper cborMapper = new ObjectMapper(new CBORFactory());
            return cborMapper.writeValueAsBytes(toCborTree(cborMapper));
        } catch (final JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot compute policy id.", e);
        }
    }

    public static PolicyScript fromCborTree(final JsonNode root) {
        if (root.size() == 2) {
            return fromCborTree(root.get(1), null);
        }
        throw new IllegalArgumentException("Invalid root node.");
    }

    private static PolicyScript fromCborTree(final JsonNode root, final PolicyScript parentPolicyScript) {
        if (root.getNodeType() == JsonNodeType.ARRAY) {
            final ArrayNode arrayNode = (ArrayNode) root;
            if (!arrayNode.isEmpty()) {
                final PolicyScript policyScript = new PolicyScript();
                final JsonNode typeNode = arrayNode.get(0);
                if (typeNode.getNodeType() == JsonNodeType.NUMBER) {
                    policyScript.setType(NativeScriptType.fromCborLabel(typeNode.asInt()));
                    if (policyScript.type == NativeScriptType.SCRIPT_N_OF_K) {
                        policyScript.setRequired(arrayNode.get(1).asInt());
                    }
                    final List<PolicyScript> childPolicyScripts = new ArrayList<>();
                    for (int childIndex = (policyScript.type == NativeScriptType.SCRIPT_N_OF_K) ? 2 : 1; childIndex < arrayNode.size(); ++childIndex) {
                        final JsonNode childNode = arrayNode.get(childIndex);
                        if (childNode.getNodeType() == JsonNodeType.ARRAY) {
                            final PolicyScript childScript = fromCborTree(childNode, policyScript);
                            childPolicyScripts.add(childScript);
                        } else {
                            if (policyScript.type == NativeScriptType.SCRIPT_PUBKEY) {
                                policyScript.setKeyHash(Hex.toHexString(Base64.decode(childNode.asText())));
                            } else if (policyScript.type == NativeScriptType.INVALID_BEFORE || policyScript.type == NativeScriptType.INVALID_HEREAFTER) {
                                policyScript.setSlot(childNode.asInt());
                            }
                        }
                    }
                    if (policyScript.getScripts() == null) {
                        if (!childPolicyScripts.isEmpty()) {
                            policyScript.setScripts(childPolicyScripts);
                        }
                    }
                    return policyScript;
                } else {
                    if (parentPolicyScript != null) {
                        final List<PolicyScript> childPolicyScripts = new ArrayList<>();
                        for (int childIndex = 0; childIndex < arrayNode.size(); ++childIndex) {
                            final JsonNode childNode = arrayNode.get(childIndex);
                            if (childNode.getNodeType() == JsonNodeType.ARRAY) {
                                final ArrayNode childArrayNode = (ArrayNode) childNode;
                                if (childArrayNode.size() == 2) {
                                    final NativeScriptType childScriptType = NativeScriptType.fromCborLabel(childArrayNode.get(0).asInt());
                                    final PolicyScript childPolicyScript = new PolicyScript();
                                    childPolicyScript.setType(childScriptType);
                                    if (childScriptType == NativeScriptType.SCRIPT_PUBKEY) {
                                        childPolicyScript.setKeyHash(Hex.toHexString(Base64.decode(childArrayNode.get(1).asText())));
                                    } else if (childScriptType == NativeScriptType.INVALID_BEFORE || childScriptType == NativeScriptType.INVALID_HEREAFTER) {
                                        childPolicyScript.setSlot(childArrayNode.get(1).asInt());
                                    }
                                    childPolicyScripts.add(childPolicyScript);
                                } else {
                                    throw new IllegalArgumentException("Leaf node of policy script definition is wrong");
                                }
                            }
                        }
                        parentPolicyScript.setScripts(childPolicyScripts);
                        return null;
                    } else {
                        throw new IllegalArgumentException("Lead node not expected at this point.");
                    }
                }
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
}
