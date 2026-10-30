package org.cardanofoundation.tokenmetadata.registry.api.service;

import lombok.RequiredArgsConstructor;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip113.ProgrammableTokenCip113;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip68.FungibleTokenMetadata;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip68.ParseOutcome;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip68.ParsedCip68Datum;
import org.cardanofoundation.tokenmetadata.registry.api.model.v2.LongProperty;
import org.cardanofoundation.tokenmetadata.registry.api.model.v2.Metadata;
import org.cardanofoundation.tokenmetadata.registry.api.model.v2.StringProperty;
import org.cardanofoundation.tokenmetadata.registry.api.model.v2.ValidationScope;
import org.cardanofoundation.tokenmetadata.registry.api.model.validate.Cip113ValidateRequest;
import org.cardanofoundation.tokenmetadata.registry.api.model.validate.Cip26ValidateRequest;
import org.cardanofoundation.tokenmetadata.registry.api.model.validate.Cip68ValidateRequest;
import org.cardanofoundation.tokenmetadata.registry.api.model.validate.DropReason;
import org.cardanofoundation.tokenmetadata.registry.api.model.validate.ServedAs;
import org.cardanofoundation.tokenmetadata.registry.api.model.validate.ValidateResponse;
import org.cardanofoundation.tokenmetadata.registry.api.service.cip113.Cip113RegistryNodeParser;
import org.cardanofoundation.tokenmetadata.registry.api.util.AssetType;
import org.cardanofoundation.tokenmetadata.registry.entity.MetadataReferenceNft;
import org.cardanofoundation.tokenmetadata.registry.entity.TokenMetadata;
import org.cardanofoundation.tokenmetadata.registry.api.model.QueryPriority;
import org.cardanofoundation.tokenmetadata.registry.invalidtoken.InvalidTokenReason;
import org.cardanofoundation.tokenmetadata.registry.model.Mapping;
import org.cardanofoundation.tokenmetadata.registry.util.Cip26Validation;
import org.cardanofoundation.tokenmetadata.registry.util.MappingsUtil;
import org.cardanofoundation.tokenmetadata.registry.util.TokenMetadataValidator;
import org.cardanofoundation.tokenmetadata.registry.validation.ValidationError;
import org.springframework.stereotype.Service;

import jakarta.annotation.Nullable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import static org.cardanofoundation.tokenmetadata.registry.api.model.cip68.Cip68Constants.LABEL_FT;
import static org.cardanofoundation.tokenmetadata.registry.api.model.cip68.Cip68Constants.LABEL_RFT;
import static org.cardanofoundation.tokenmetadata.registry.api.model.cip68.Cip68Constants.REFERENCE_TOKEN_PREFIX;

/**
 * Answers "what would the registry do with this?" for a CIP-26 registry entry, a CIP-68 reference NFT datum or a
 * CIP-113 registry node, without storing anything (ADR-023). It runs the code the indexer runs, so the answer cannot
 * drift from what the registry does: the CIP-26 validator, the CIP-68 parser and per-label rules with the mapping V2
 * serves, and the CIP-113 node parser.
 */
@Service
@RequiredArgsConstructor
public class TokenValidationService {

    /** A placeholder policy for a datum that has none yet: it is only used to build the row the preview is served from. */
    private static final AssetType PREVIEW_REFERENCE_NFT = new AssetType("00".repeat(28), REFERENCE_TOKEN_PREFIX);
    private static final String CIP_68_SUBJECT_NOTE = "V2 serves this metadata under policy id + 0014df10 (333) or "
            + "001bc280 (444) + the name of the reference NFT, whether or not that user token is minted (ADR-022)";

    private final TokenMetadataValidator tokenMetadataValidator;
    private final Cip68FTDatumParser cip68DatumParser;
    private final Cip68FungibleTokenService cip68FungibleTokenService;
    private final Cip113RegistryNodeParser cip113RegistryNodeParser;

    public ValidateResponse validateCip26(Cip26ValidateRequest request) {
        Mapping mapping = request.mapping();
        if (mapping == null) {
            throw new IllegalArgumentException("mapping is required");
        }
        List<String> notes = List.of("Signatures are not verified: the check covers the content of the entry");
        if (request.fileName() != null && !stripJson(request.fileName()).equalsIgnoreCase(String.valueOf(mapping.subject()))) {
            return dropped(InvalidTokenReason.CIP_26, new DropReason(InvalidTokenReason.FILENAME_SUBJECT_MISMATCH,
                    "the file is named after another subject than the one inside it, '" + mapping.subject()
                            + "': the registry indexes it under neither"), null, notes);
        }

        TokenMetadata tokenMetadata = MappingsUtil.toTokenMetadata(mapping, "validator", LocalDateTime.now());
        Cip26Validation validation = tokenMetadataValidator.validate(mapping, tokenMetadata);
        if (validation.isDropped()) {
            return dropped(InvalidTokenReason.CIP_26, new DropReason(InvalidTokenReason.UNUSABLE_SUBJECT, validation.droppedReason()),
                    null, notes);
        }
        String logo = MappingsUtil.toTokenLogo(mapping).getLogo();
        Cip26Validation logoValidation = tokenMetadataValidator.validateLogo(tokenMetadata.getSubject(), logo);
        List<ValidationError> errors = new ArrayList<>(validation.errors());
        errors.addAll(logoValidation.errors());
        if (logoValidation.isDropped()) {
            notes = List.of(notes.getFirst(), "The logo would not be stored: " + logoValidation.droppedReason());
            logo = null;
        }

        ServedAs served = served(null, cip26Metadata(tokenMetadata, logo), errors);
        return new ValidateResponse(InvalidTokenReason.CIP_26, true, null, errors, List.of(served), null, null, notes);
    }

    public ValidateResponse validateCip68(Cip68ValidateRequest request) {
        if ((request.datum() == null) == (request.fields() == null)) {
            throw new IllegalArgumentException("give either datum (CBOR hex) or fields");
        }
        List<Integer> labels = request.labels() == null || request.labels().isEmpty()
                ? List.of(LABEL_FT)
                : List.copyOf(new LinkedHashSet<>(request.labels()));
        if (labels.stream().anyMatch(label -> label != LABEL_FT && label != LABEL_RFT)) {
            throw new IllegalArgumentException("labels can be 333 (fungible token) and 444 (rich fungible token); "
                    + "this registry does not serve NFTs (222)");
        }
        String datum = request.datum() != null ? request.datum().trim() : Cip68DatumBuilder.build(request.fields());

        ParseOutcome outcome = cip68DatumParser.parseDetailed(datum, PREVIEW_REFERENCE_NFT);
        return switch (outcome) {
            case ParseOutcome.NotCip68 _ -> dropped(InvalidTokenReason.CIP_68, new DropReason("not_cip68",
                    "the datum is not a CIP-68 datum (Constr 0 [metadata map, version, ...]): the registry ignores it"), datum, List.of());
            case ParseOutcome.Dropped(String reasonCode, String message) ->
                    dropped(InvalidTokenReason.CIP_68, new DropReason(reasonCode, message), datum, List.of());
            case ParseOutcome.Parsed(ParsedCip68Datum parsed) -> parsedCip68(parsed, labels, datum);
        };
    }

    private ValidateResponse parsedCip68(ParsedCip68Datum parsed, List<Integer> labels, String datum) {
        List<ValidationError> errors = Cip68EventListener.errorsForLabels(parsed, labels);
        // The row carries the datum's own errors, as the indexer stores them: the label's are added when it is served.
        MetadataReferenceNft row = Cip68EventListener.buildMetadataReferenceNft(parsed, Cip68EventListener.datumErrors(parsed),
                PREVIEW_REFERENCE_NFT, datum, 0L);
        List<ServedAs> served = labels.stream()
                .map(label -> {
                    FungibleTokenMetadata metadata = cip68FungibleTokenService.toFungibleTokenMetadata(row, List.of(), label == LABEL_RFT);
                    return served(label, Metadata.from(metadata), metadata.validationErrors());
                })
                .toList();
        return new ValidateResponse(InvalidTokenReason.CIP_68, true, null, errors, served, datum, null, List.of(CIP_68_SUBJECT_NOTE));
    }

    public ValidateResponse validateCip113(Cip113ValidateRequest request) {
        if (request.datum() == null || request.datum().isBlank()) {
            throw new IllegalArgumentException("datum (CBOR hex) is required");
        }
        Cip113RegistryNodeParser.ParseResult result = cip113RegistryNodeParser.parseDetailed(request.datum().trim());
        if (result.node() == null) {
            return dropped(InvalidTokenReason.CIP_113, new DropReason(InvalidTokenReason.REGISTRY_NODE_UNPARSEABLE,
                    String.valueOf(result.rejection())), null, List.of());
        }
        Cip113RegistryNodeParser.ParsedRegistryNode node = result.node();
        List<String> notes = new ArrayList<>();
        notes.add("A programmable token is served only with its CIP-26 or CIP-68 metadata: check that metadata too");
        if (node.isHeadSentinel()) {
            notes.add("This is the head of the registry's linked list, not a registered policy");
        }
        ProgrammableTokenCip113 extension = new ProgrammableTokenCip113(emptyToNull(node.mintingLogicScript()),
                emptyToNull(node.transferLogicScript()), emptyToNull(node.thirdPartyTransferLogicScript()),
                emptyToNull(node.unfrackingLogicScript()), emptyToNull(node.globalStatePolicyId()));
        return new ValidateResponse(InvalidTokenReason.CIP_113, true, null, List.of(), List.of(), null, extension, notes);
    }

    private static ValidateResponse dropped(String standard, DropReason reason, @Nullable String datum, List<String> notes) {
        return new ValidateResponse(standard, false, reason, List.of(), List.of(), datum, null, notes);
    }

    private static ServedAs served(@Nullable Integer label, Metadata metadata, List<ValidationError> errors) {
        List<ValidationScope> scopes = Arrays.stream(ValidationScope.values())
                .filter(scope -> scope.accepts(errors, true))
                .toList();
        return new ServedAs(label, metadata.toBuilder().valid(errors.isEmpty()).build(), errors, scopes);
    }

    private static Metadata cip26Metadata(TokenMetadata metadata, @Nullable String logo) {
        String source = QueryPriority.CIP_26.name();
        return Metadata.builder()
                .name(text(metadata.getName(), source))
                .description(text(metadata.getDescription(), source))
                .ticker(text(metadata.getTicker(), source))
                .url(text(metadata.getUrl(), source))
                .logo(text(logo, source))
                .decimals(metadata.getDecimals() != null ? new LongProperty(metadata.getDecimals(), source) : null)
                .build();
    }

    @Nullable
    private static StringProperty text(@Nullable String value, String source) {
        return value != null ? new StringProperty(value, source) : null;
    }

    private static String stripJson(String fileName) {
        return fileName.endsWith(".json") ? fileName.substring(0, fileName.length() - ".json".length()) : fileName;
    }

    @Nullable
    private static String emptyToNull(@Nullable String value) {
        return value == null || value.isEmpty() ? null : value;
    }
}
