package org.cardanofoundation.tokenmetadata.registry.api.service;

import jakarta.annotation.Nullable;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip68.Cip68Uri;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip68.ParsedCip68Datum;
import org.cardanofoundation.tokenmetadata.registry.validation.FieldCategory;
import org.cardanofoundation.tokenmetadata.registry.validation.ValidationCode;
import org.cardanofoundation.tokenmetadata.registry.validation.ValidationError;

import java.util.ArrayList;
import java.util.List;

import static org.cardanofoundation.tokenmetadata.registry.api.model.cip68.Cip68Constants.LABEL_FT;
import static org.cardanofoundation.tokenmetadata.registry.validation.ValidationError.CIP_68;

/**
 * Checks a parsed CIP-68 datum against what CIP-68 requires of the label of the token, and reports what is missing or
 * wrong, as yaci-store does. Nothing is rejected: the token is indexed and served either way, with these errors
 * (ADR-019), so that its owner can fix them and a wallet can decide whether to accept the token.
 * <ul>
 *   <li>{@code name} for every label;</li>
 *   <li>{@code description} for the 333 fungible token only (for 222 and 444 it is {@code ? description}). A
 *       description of the wrong type, or given as chunks, is reported once per label, with the category of that
 *       label;</li>
 *   <li>{@code image} for the 222 NFT and the 444 RFT, as a URI whose scheme is one of {@code https}, {@code ipfs},
 *       {@code ar} or {@code data}. A missing or empty image, or another scheme (for example {@code iagon://}, or free
 *       text), is an error: wallets and explorers follow the CIP and could not show such a token.</li>
 * </ul>
 * The optional {@code logo} must also be such a URI; that does not depend on the label.
 */
public final class Cip68Validator {

    private static final int ABBREVIATED_LENGTH = 60;
    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";

    private Cip68Validator() {
    }

    /**
     * The errors of the fields {@code label} requires. A reference NFT with user tokens of several labels is checked
     * once per label, and each error carries the label it was found for in {@code sub_standard}.
     *
     * @param parsed the parsed datum
     * @param label  the user-token label the datum belongs to: {@code 222} (NFT), {@code 333} (FT) or {@code 444} (RFT)
     */
    public static List<ValidationError> validate(ParsedCip68Datum parsed, int label) {
        String subStandard = String.valueOf(label);
        List<ValidationError> errors = new ArrayList<>();
        // a name of the wrong type is already reported by the parser as mandatory, so it is not reported as missing too
        if (parsed.name() == null && !reportedByParser(parsed, NAME)) {
            errors.add(missing(subStandard, NAME));
        }
        // the form of a description (a wrong type, chunks) is reported here, once per label, with the category of the
        // label, instead of by the parser: only the 333 fungible token requires a description
        boolean descriptionRequired = label == LABEL_FT;
        ValidationError descriptionForm = descriptionFormError(parsed);
        if (descriptionForm != null) {
            errors.add(new ValidationError(CIP_68, subStandard, DESCRIPTION,
                    descriptionRequired ? FieldCategory.MANDATORY : FieldCategory.OPTIONAL,
                    descriptionForm.code(), descriptionForm.error()));
        } else if (descriptionRequired && parsed.description() == null) {
            errors.add(missing(subStandard, DESCRIPTION));
        }
        if (label == LABEL_FT) {
            return errors;
        }
        String image = parsed.image();
        if (image == null || image.isBlank()) {
            errors.add(missing(subStandard, "image"));
        } else if (!Cip68Uri.hasAllowedScheme(image)) {
            errors.add(ValidationError.mandatory(CIP_68, subStandard, "image", ValidationCode.INVALID_URI_SCHEME,
                    "image '" + abbreviate(image) + "' is not a URI with one of the schemes CIP-68 allows ("
                            + Cip68Uri.ALLOWED_SCHEMES + ")"));
        }
        return errors;
    }

    /**
     * The parser's errors that do not depend on the label. The form of the {@code description} is left out: whether
     * it is mandatory depends on the label, so {@link #validate} reports it once per label.
     */
    public static List<ValidationError> labelIndependentErrors(ParsedCip68Datum parsed) {
        return parsed.validationErrors().stream().filter(error -> !isDescriptionForm(error)).toList();
    }

    /**
     * The error of a {@code logo} that is not a URI with a scheme CIP-68 allows (a bare IPFS hash, {@code iagon://}, a
     * base64 string). The logo is optional, so it is kept as written and reported; it does not depend on the label.
     */
    @Nullable
    public static ValidationError validateLogo(ParsedCip68Datum parsed) {
        String logo = parsed.logo();
        if (logo == null || logo.isBlank() || Cip68Uri.hasAllowedScheme(logo)) {
            return null;
        }
        return ValidationError.optional(CIP_68, null, "logo", ValidationCode.INVALID_URI_SCHEME,
                "logo '" + abbreviate(logo) + "' is not a URI with one of the schemes CIP-68 allows (" + Cip68Uri.ALLOWED_SCHEMES + ")");
    }

    private static boolean reportedByParser(ParsedCip68Datum parsed, String field) {
        return parsed.validationErrors().stream().anyMatch(error -> field.equals(error.fieldName()));
    }

    @Nullable
    private static ValidationError descriptionFormError(ParsedCip68Datum parsed) {
        return parsed.validationErrors().stream().filter(Cip68Validator::isDescriptionForm).findFirst().orElse(null);
    }

    private static boolean isDescriptionForm(ValidationError error) {
        return DESCRIPTION.equals(error.fieldName())
                && (ValidationCode.WRONG_TYPE.equals(error.code()) || ValidationCode.CHUNKED_TEXT.equals(error.code()));
    }

    private static ValidationError missing(String subStandard, String field) {
        return ValidationError.mandatory(CIP_68, subStandard, field, ValidationCode.MISSING_VALUE,
                field + " field is missing a mandatory value");
    }

    private static String abbreviate(String value) {
        String oneLine = value.replaceAll("\\s+", " ");
        return oneLine.length() <= ABBREVIATED_LENGTH ? oneLine : oneLine.substring(0, ABBREVIATED_LENGTH) + "...";
    }
}
