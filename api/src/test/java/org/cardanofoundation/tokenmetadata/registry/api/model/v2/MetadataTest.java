package org.cardanofoundation.tokenmetadata.registry.api.model.v2;

import org.cardanofoundation.tokenmetadata.registry.api.model.QueryPriority;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip68.FungibleTokenMetadata;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.TokenMetadata;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.wellknownproperties.NameProperty;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.wellknownproperties.TickerProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.cardanofoundation.tokenmetadata.registry.api.model.QueryPriority.CIP_26;
import static org.cardanofoundation.tokenmetadata.registry.api.model.QueryPriority.CIP_68;

class MetadataTest {

    @Test
    public void testFromTokenMetadata() {

        var nameProperty = new NameProperty();
        nameProperty.setValue("name");

        var tickerProperty = new TickerProperty();
        tickerProperty.setValue("ticker");

        var actual = Metadata.from(TokenMetadata.builder()
                .name(nameProperty)
                .ticker(tickerProperty)
                .build());
        var expected = Metadata.builder()
                .name(new StringProperty("name", CIP_26.name()))
                .ticker(new StringProperty("ticker", CIP_26.name()))
                .build();
        Assertions.assertEquals(expected, actual);

    }

    @Test
    public void testFromFungibleTokenMetadata() {

        var nameProperty = new NameProperty();
        nameProperty.setValue("name");

        var tickerProperty = new TickerProperty();
        tickerProperty.setValue("ticker");

        var actual = Metadata.from(new FungibleTokenMetadata(9L, "description", "logo" , "name", "ticker", null, 1L));

        var expected = Metadata.builder()
                .name(new StringProperty("name", CIP_68.name()))
                .ticker(new StringProperty("ticker", CIP_68.name()))
                .description(new StringProperty("description", CIP_68.name()))
                .logo(new StringProperty("logo", CIP_68.name()))
                .decimals(new LongProperty(9L, CIP_68.name()))
                .version(new LongProperty(1L, CIP_68.name()))
                .build();

        Assertions.assertEquals(expected, actual);

    }


}