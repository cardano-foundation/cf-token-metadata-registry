package org.cardanofoundation.tokenmetadata.registry.api.util;

import com.bloxbean.cardano.client.util.HexUtil;
import com.fasterxml.jackson.annotation.JsonKey;
import lombok.extern.slf4j.Slf4j;

/**
 * Record/Utility class to group in one place Asset unit, policy and name manipulation and comparison
 *
 * @param policyId
 * @param assetName
 */
@Slf4j
public record AssetType(String policyId, String assetName) {

    private static final String ADA = "ada";

    private static final String LOVELACE = "lovelace";

    private static final AssetType Ada = new AssetType("", LOVELACE);

    public String toUnit() {
        return policyId + assetName;
    }

    public boolean isAda() {
        return this.equals(Ada);
    }

    @JsonKey
    public String unsafeHumanAssetName() {
        if (this.isAda()) {
            return ADA;
        } else {
            return new String(HexUtil.decodeHexString(assetName));
        }
    }

    public static AssetType fromUnit(String unit) {
        try {
            if (unit.equalsIgnoreCase(LOVELACE) || unit.trim().isEmpty()) {
                return Ada;
            } else {
                String sanitizedUnit = unit.replaceAll("\\.", "");
                if (sanitizedUnit.length() > 56) {
                    return new AssetType(sanitizedUnit.substring(0, 56), sanitizedUnit.substring(56));
                } else {
                    return new AssetType(sanitizedUnit.substring(0, 56), "");
                }
            }
        } catch (Exception e) {
            log.warn("Invalid unit '{}'", unit);
            throw new RuntimeException(e);
        }

    }

    public static AssetType ada() {
        return Ada;
    }

}
