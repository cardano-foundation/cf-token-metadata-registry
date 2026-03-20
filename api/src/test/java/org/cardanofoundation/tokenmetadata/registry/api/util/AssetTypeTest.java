package org.cardanofoundation.tokenmetadata.registry.api.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class AssetTypeTest {

    private static final String POLICY_ID_56 = "f6f49b186751e61f1fb8c64e7504e771f968cea9f4d11f5222b169e3";
    private static final String ASSET_NAME_HEX = "744d494e";

    @Nested
    @DisplayName("fromUnit")
    class FromUnit {

        @Nested
        @DisplayName("lovelace / empty handling")
        class LovelaceAndEmpty {

            @Test
            void lovelace_returnsAda() {
                var result = AssetType.fromUnit("lovelace");

                assertThat(result.isAda()).isTrue();
                assertThat(result.policyId()).isEmpty();
                assertThat(result.assetName()).isEqualTo("lovelace");
            }

            @Test
            void lovelaceUpperCase_returnsAda() {
                var result = AssetType.fromUnit("LOVELACE");

                assertThat(result.isAda()).isTrue();
            }

            @Test
            void emptyString_returnsAda() {
                var result = AssetType.fromUnit("  ");

                assertThat(result.isAda()).isTrue();
            }
        }

        @Nested
        @DisplayName("valid units (policy + asset name)")
        class ValidUnits {

            @Test
            void unitLongerThan56Chars_splitsPolicyAndAssetName() {
                String unit = POLICY_ID_56 + ASSET_NAME_HEX;

                var result = AssetType.fromUnit(unit);

                assertThat(result.policyId()).isEqualTo(POLICY_ID_56);
                assertThat(result.assetName()).isEqualTo(ASSET_NAME_HEX);
            }

            @Test
            void unitExactly56Chars_returnsPolicyWithEmptyAssetName() {
                var result = AssetType.fromUnit(POLICY_ID_56);

                assertThat(result.policyId()).isEqualTo(POLICY_ID_56);
                assertThat(result.assetName()).isEmpty();
            }

            @Test
            void unitWithDots_removesDotsBeforeParsing() {
                String unit = POLICY_ID_56 + "." + ASSET_NAME_HEX;

                var result = AssetType.fromUnit(unit);

                assertThat(result.policyId()).isEqualTo(POLICY_ID_56);
                assertThat(result.assetName()).isEqualTo(ASSET_NAME_HEX);
            }
        }

        @Nested
        @DisplayName("short / invalid units")
        class ShortUnits {

            @ParameterizedTest(name = "short unit \"{0}\" does not throw")
            @ValueSource(strings = {"abcdef", "a", "nonexistent"})
            void shortUnit_doesNotThrow_andUsesInputAsPolicyId(String unit) {
                var result = AssetType.fromUnit(unit);

                assertThat(result.policyId()).isEqualTo(unit);
                assertThat(result.assetName()).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("toUnit")
    class ToUnit {

        @Test
        void concatenatesPolicyAndAssetName() {
            var assetType = new AssetType(POLICY_ID_56, ASSET_NAME_HEX);

            assertThat(assetType.toUnit()).isEqualTo(POLICY_ID_56 + ASSET_NAME_HEX);
        }

        @Test
        void roundTripsWithFromUnit() {
            String original = POLICY_ID_56 + ASSET_NAME_HEX;

            var result = AssetType.fromUnit(original);

            assertThat(result.toUnit()).isEqualTo(original);
        }
    }

    @Nested
    @DisplayName("isAda")
    class IsAda {

        @Test
        void adaSingleton_isAda() {
            assertThat(AssetType.ada().isAda()).isTrue();
        }

        @Test
        void nonAdaAsset_isNotAda() {
            var assetType = new AssetType(POLICY_ID_56, ASSET_NAME_HEX);

            assertThat(assetType.isAda()).isFalse();
        }
    }
}
