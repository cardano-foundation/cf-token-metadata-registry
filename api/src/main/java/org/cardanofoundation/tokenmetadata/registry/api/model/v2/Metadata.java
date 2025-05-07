package org.cardanofoundation.tokenmetadata.registry.api.model.v2;

import com.bloxbean.cardano.client.util.HexUtil;
import lombok.Builder;
import org.cardanofoundation.tokenmetadata.registry.api.model.QueryPriority;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip68.FungibleTokenMetadata;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.TokenMetadata;

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



    public static Metadata from(TokenMetadata metadata) {

        var name = metadata.getName() != null ? new StringProperty(metadata.getName().getValue(), QueryPriority.CIP_26.name()) : null;
        var description = metadata.getDescription() != null ? new StringProperty(metadata.getDescription().getValue(), QueryPriority.CIP_26.name()) : null;
        var ticker = metadata.getTicker() != null ? new StringProperty(metadata.getTicker().getValue(), QueryPriority.CIP_26.name()) : null;
        var decimals = metadata.getDecimals() != null ? new LongProperty(metadata.getDecimals().getValue().longValue(), QueryPriority.CIP_26.name()) : null;
        var logo = metadata.getLogo() != null ? new StringProperty(HexUtil.encodeHexString(metadata.getLogo().getValue()), QueryPriority.CIP_26.name()) : null;
        var url = metadata.getUrl() != null ? new StringProperty(metadata.getUrl().getValue(), QueryPriority.CIP_26.name()) : null;

        return new Metadata(name, description, ticker, decimals, logo, url, null);
    }

    public static Metadata from(FungibleTokenMetadata fungibleTokenMetadata) {

        var name = fungibleTokenMetadata.name() != null ? new StringProperty(fungibleTokenMetadata.name(), QueryPriority.CIP_68.name()) : null;
        var description = fungibleTokenMetadata.description() != null ? new StringProperty(fungibleTokenMetadata.description(), QueryPriority.CIP_68.name()) : null;
        var ticker = fungibleTokenMetadata.ticker() != null ? new StringProperty(fungibleTokenMetadata.ticker(), QueryPriority.CIP_68.name()) : null;
        var decimals = fungibleTokenMetadata.decimals() != null ? new LongProperty(fungibleTokenMetadata.decimals(), QueryPriority.CIP_68.name()) : null;
        var logo = fungibleTokenMetadata.logo() != null ? new StringProperty(fungibleTokenMetadata.logo(), QueryPriority.CIP_68.name()) : null;
        var url = fungibleTokenMetadata.url() != null ? new StringProperty(fungibleTokenMetadata.url(), QueryPriority.CIP_68.name()) : null;
        var version = fungibleTokenMetadata.version() != null ? new LongProperty(fungibleTokenMetadata.version(), QueryPriority.CIP_68.name()) : null;

        return new Metadata(name, description, ticker, decimals, logo, url, version);
    }


}
