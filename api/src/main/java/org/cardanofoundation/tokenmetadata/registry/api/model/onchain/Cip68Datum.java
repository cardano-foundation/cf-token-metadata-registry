package org.cardanofoundation.tokenmetadata.registry.api.model.onchain;

import java.math.BigInteger;

public record Cip68Datum(BigInteger decimals, String description, String logo, String name, String ticker,
                         String website) {


}
