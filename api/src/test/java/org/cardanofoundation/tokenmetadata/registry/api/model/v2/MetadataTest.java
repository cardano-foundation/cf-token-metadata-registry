package org.cardanofoundation.tokenmetadata.registry.api.model.v2;

import org.cardanofoundation.tokenmetadata.registry.api.model.cip68.FungibleTokenMetadata;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.TokenMetadata;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.wellknownproperties.LogoProperty;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.wellknownproperties.NameProperty;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.wellknownproperties.TickerProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.cardanofoundation.tokenmetadata.registry.api.model.QueryPriority.CIP_26;
import static org.cardanofoundation.tokenmetadata.registry.api.model.QueryPriority.CIP_68;

class MetadataTest {

    @Test
    void testFromTokenMetadata() {

        NameProperty nameProperty = new NameProperty();
        nameProperty.setValue("name");

        TickerProperty tickerProperty = new TickerProperty();
        tickerProperty.setValue("ticker");

        Metadata actual = Metadata.from(TokenMetadata.builder()
                .name(nameProperty)
                .ticker(tickerProperty)
                .build());
        Metadata expected = Metadata.builder()
                .name(new StringProperty("name", CIP_26.name()))
                .ticker(new StringProperty("ticker", CIP_26.name()))
                .build();
        Assertions.assertEquals(expected, actual);

    }

    @Test
    void testFromFungibleTokenMetadata() {

        NameProperty nameProperty = new NameProperty();
        nameProperty.setValue("name");

        TickerProperty tickerProperty = new TickerProperty();
        tickerProperty.setValue("ticker");

        Metadata actual = Metadata.from(new FungibleTokenMetadata(9L, "description", "logo" , "name", "ticker", null, 1L));

        Metadata expected = Metadata.builder()
                .name(new StringProperty("name", CIP_68.name()))
                .ticker(new StringProperty("ticker", CIP_68.name()))
                .description(new StringProperty("description", CIP_68.name()))
                .logo(new StringProperty("logo", CIP_68.name()))
                .decimals(new LongProperty(9L, CIP_68.name()))
                .version(new LongProperty(1L, CIP_68.name()))
                .build();

        Assertions.assertEquals(expected, actual);

    }

    @Test
    void logoFromCip26IsBase64Encoded() {
        // CIP-26 spec mandates base64 logos. Same shape as standards.cip26.logo (which is the
        // raw byte[] property — Jackson default-serializes that as base64). The flat metadata
        // block was historically hex-encoding the bytes here, which diverged from both the
        // spec and the standards block in the same response. Lock in base64.
        NameProperty nameProperty = new NameProperty();
        nameProperty.setValue("name");

        byte[] logoBytes = new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'}; // PNG magic
        LogoProperty logoProperty = new LogoProperty();
        logoProperty.setValue(logoBytes);

        Metadata actual = Metadata.from(TokenMetadata.builder()
                .name(nameProperty)
                .logo(logoProperty)
                .build());

        String expectedBase64 = Base64.getEncoder().encodeToString(logoBytes);
        Assertions.assertEquals(new StringProperty(expectedBase64, CIP_26.name()), actual.logo());
    }

}