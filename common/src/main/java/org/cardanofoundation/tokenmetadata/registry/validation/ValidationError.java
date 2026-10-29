package org.cardanofoundation.tokenmetadata.registry.validation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

/**
 * One way in which the metadata of a token does not follow its CIP. The token is indexed and served as written; these
 * entries tell the owner what to fix and let a wallet decide whether to accept the token (ADR-019). They are stored as
 * JSON next to the row and returned as {@code validation_errors} by the V2 API.
 *
 * @param cip           the standard the rule comes from, {@link #CIP_26}, {@link #CIP_68} or {@link #CIP_113}
 * @param subStandard   the CIP-68 label the rule was checked for ({@code "222"}, {@code "333"}, {@code "444"});
 *                      {@code null} for CIP-26 and for rules that do not depend on a label
 * @param fieldName     the metadata field, for example {@code description} or {@code properties.owner}
 * @param fieldCategory whether the CIP requires the field
 * @param code          a stable machine-readable reason, see {@link ValidationCode}
 * @param error         a human-readable message
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ValidationError(
        @JsonProperty("cip") String cip,
        @JsonProperty("sub_standard") @Nullable String subStandard,
        @JsonProperty("field_name") String fieldName,
        @JsonProperty("field_category") FieldCategory fieldCategory,
        @JsonProperty("code") String code,
        @JsonProperty("error") String error) {

    public static final String CIP_26 = "CIP-26";
    public static final String CIP_68 = "CIP-68";
    public static final String CIP_113 = "CIP-113";

    public static ValidationError mandatory(String cip, @Nullable String subStandard, String fieldName, String code,
                                            String error) {
        return new ValidationError(cip, subStandard, fieldName, FieldCategory.MANDATORY, code, error);
    }

    public static ValidationError optional(String cip, @Nullable String subStandard, String fieldName, String code,
                                           String error) {
        return new ValidationError(cip, subStandard, fieldName, FieldCategory.OPTIONAL, code, error);
    }
}
