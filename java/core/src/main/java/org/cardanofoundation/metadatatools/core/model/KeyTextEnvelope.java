package org.cardanofoundation.metadatatools.core.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.bouncycastle.util.encoders.Hex;

@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.PUBLIC)
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class KeyTextEnvelope {
    @JsonIgnore
    private KeyTextEnvelopeType type;

    @JsonIgnore
    private byte[] cbor;

    @JsonProperty("type")
    final public String getTypeValue() {
        return this.type.getType();
    }

    @JsonProperty("type")
    final public void setTypeValue(final String typeValue) {
        this.type = KeyTextEnvelopeType.fromTypeValue(typeValue);
    }

    @JsonIgnore
    @JsonProperty("description")
    public void setDescription(final String description) {
    }

    @JsonProperty("description")
    final public String getDescription() {
        return this.type.getDescription();
    }

    @JsonProperty("cborHex")
    final public String getCborHex() {
        return Hex.toHexString(this.cbor);
    }

    @JsonProperty("cborHex")
    final public void setCborHex(final String value) {
        this.cbor = Hex.decode(value);
    }
}
