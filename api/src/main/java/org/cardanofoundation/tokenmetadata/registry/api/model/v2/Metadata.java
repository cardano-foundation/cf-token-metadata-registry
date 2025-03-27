package org.cardanofoundation.tokenmetadata.registry.api.model.v2;

import lombok.Builder;

@Builder(toBuilder = true)
public record Metadata(StringProperty name, StringProperty description, StringProperty ticker, LongProperty decimals,
                       StringProperty logo, StringProperty url, LongProperty version) {

    public static Metadata empty() {
        return Metadata.builder().build();
    }

    public Metadata merge(Metadata that) {
        return new Metadata(name != null ? name : that.name(),
                description != null ? description : that.description(),
                ticker != null ? ticker : that.ticker(),
                decimals != null ? decimals : that.decimals(),
                logo != null ? logo : that.logo(),
                url != null ? url : that.url(),
                version != null ? version : that.version());
    }

}
