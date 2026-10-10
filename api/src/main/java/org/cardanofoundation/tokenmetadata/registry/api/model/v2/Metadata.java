package org.cardanofoundation.tokenmetadata.registry.api.model.v2;

import lombok.Builder;
import org.cardanofoundation.tokenmetadata.registry.api.model.QueryPriority;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip68.FungibleTokenMetadata;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.TokenMetadata;

import java.util.Base64;

@Builder(toBuilder = true)
public record Metadata(StringProperty name, StringProperty description, StringProperty ticker, LongProperty decimals,
                       StringProperty logo, StringProperty url, LongProperty version) {

    private static final Metadata EMPTY_METADATA = Metadata.builder().build();

    public static Metadata empty() {
        return EMPTY_METADATA;
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


    public static Metadata from(TokenMetadata metadata) {

        StringProperty name = metadata.getName() != null ? new StringProperty(metadata.getName().getValue(), QueryPriority.CIP_26.name()) : null;
        StringProperty description = metadata.getDescription() != null ? new StringProperty(metadata.getDescription().getValue(), QueryPriority.CIP_26.name()) : null;
        StringProperty ticker = metadata.getTicker() != null ? new StringProperty(metadata.getTicker().getValue(), QueryPriority.CIP_26.name()) : null;
        LongProperty decimals = metadata.getDecimals() != null ? new LongProperty(metadata.getDecimals().getValue().longValue(), QueryPriority.CIP_26.name()) : null;
        // CIP-26 spec: logo bytes must be base64-encoded. Was previously hex-encoded here, which
        // diverged from both the spec and from this same response's standards.cip26.logo block
        // (Jackson's default byte[] serialization is base64). Standardise on base64 everywhere.
        StringProperty logo = metadata.getLogo() != null ? new StringProperty(Base64.getEncoder().encodeToString(metadata.getLogo().getValue()), QueryPriority.CIP_26.name()) : null;
        StringProperty url = metadata.getUrl() != null ? new StringProperty(metadata.getUrl().getValue(), QueryPriority.CIP_26.name()) : null;

        return new Metadata(name, description, ticker, decimals, logo, url, null);
    }

    public static Metadata from(FungibleTokenMetadata fungibleTokenMetadata) {

        StringProperty name = fungibleTokenMetadata.name() != null ? new StringProperty(fungibleTokenMetadata.name(), QueryPriority.CIP_68.name()) : null;
        StringProperty description = fungibleTokenMetadata.description() != null ? new StringProperty(fungibleTokenMetadata.description(), QueryPriority.CIP_68.name()) : null;
        StringProperty ticker = fungibleTokenMetadata.ticker() != null ? new StringProperty(fungibleTokenMetadata.ticker(), QueryPriority.CIP_68.name()) : null;
        LongProperty decimals = fungibleTokenMetadata.decimals() != null ? new LongProperty(fungibleTokenMetadata.decimals(), QueryPriority.CIP_68.name()) : null;
        StringProperty logo = fungibleTokenMetadata.logo() != null ? new StringProperty(fungibleTokenMetadata.logo(), QueryPriority.CIP_68.name()) : null;
        StringProperty url = fungibleTokenMetadata.url() != null ? new StringProperty(fungibleTokenMetadata.url(), QueryPriority.CIP_68.name()) : null;
        LongProperty version = fungibleTokenMetadata.version() != null ? new LongProperty(fungibleTokenMetadata.version(), QueryPriority.CIP_68.name()) : null;

        return new Metadata(name, description, ticker, decimals, logo, url, version);
    }

    public boolean isEmpty() {
        return this.equals(EMPTY_METADATA);
    }

    public boolean isValid() {
        return this.name != null && this.description != null;
    }

}
