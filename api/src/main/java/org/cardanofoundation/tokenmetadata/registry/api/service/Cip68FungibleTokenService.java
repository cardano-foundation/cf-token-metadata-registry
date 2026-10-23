package org.cardanofoundation.tokenmetadata.registry.api.service;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip68.FungibleTokenMetadata;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip68.ParsedCip68Datum;
import org.cardanofoundation.tokenmetadata.registry.api.util.AssetType;
import org.cardanofoundation.tokenmetadata.registry.repository.MetadataReferenceNftRepository;
import org.cardanofoundation.tokenmetadata.registry.validation.ValidationError;
import org.springframework.stereotype.Service;

import jakarta.annotation.Nullable;
import org.cardanofoundation.tokenmetadata.registry.entity.MetadataReferenceNft;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.cardanofoundation.tokenmetadata.registry.api.model.cip68.Cip68Constants.FUNGIBLE_TOKEN_PREFIX;
import static org.cardanofoundation.tokenmetadata.registry.api.model.cip68.Cip68Constants.LABEL_FT;
import static org.cardanofoundation.tokenmetadata.registry.api.model.cip68.Cip68Constants.LABEL_RFT;
import static org.cardanofoundation.tokenmetadata.registry.api.model.cip68.Cip68Constants.REFERENCE_TOKEN_PREFIX;
import static org.cardanofoundation.tokenmetadata.registry.api.model.cip68.Cip68Constants.RICH_FUNGIBLE_TOKEN_PREFIX;

@Service
@RequiredArgsConstructor
@Slf4j
public class Cip68FungibleTokenService {

    // This represents the hex encoding of `(100)` the prefix in the name of the Reference Token
    private static final String REFERENCE_NFT_PREFIX = "000643b0";

    private static final String VERSION = "version";

    private final MetadataReferenceNftRepository metadataReferenceNftRepository;
    private final Cip68FTDatumParser cip68DatumParser;

    /**
     * Checks whether the utxo contains an NFT which matches Cip68 Reference NFT requirements
     *
     * @param utxo the utxo to check
     * @return true if any of the utxo's contains a Cip68 Reference NFT
     */
    public boolean containsReferenceNft(AddressUtxo utxo) {
        return utxo.getAmounts().stream().anyMatch(this::isReferenceNft);
    }

    /**
     * Returns every reference NFT in the output, in the order of its assets. An output normally holds one, but a
     * nested datum can describe several, which is why they may be locked together.
     *
     * @param utxo the utxo to check
     * @return the amounts matching Cip68 Reference NFT requirements, empty if there are none
     */
    public List<Amt> extractReferenceNfts(AddressUtxo utxo) {
        return utxo.getAmounts().stream().filter(this::isReferenceNft).toList();
    }

    /**
     * Check if the amount matches Cip68 Reference NFT Requirements
     *
     * @param amount the amount to check
     * @return true if the amount is a Cip68 Reference NFT
     */
    public boolean isReferenceNft(Amt amount) {
        return amount.getQuantity().equals(BigInteger.ONE)
                && AssetType.fromUnit(amount.getUnit()).assetName().startsWith(REFERENCE_NFT_PREFIX);
    }

    /**
     * Batch-fetches the latest CIP-68 reference NFT metadata for a set of policy IDs.
     * Returns a map keyed by "policyId:assetName" for O(1) lookup.
     */
    public Map<String, MetadataReferenceNft> findLatestByPolicyIds(Collection<String> policyIds) {
        return metadataReferenceNftRepository.findLatestByPolicyIds(policyIds).stream()
                .collect(Collectors.toMap(
                        nft -> nft.getPolicyId() + ":" + nft.getAssetName(),
                        nft -> nft,
                        (a, b) -> a));
    }

    /**
     * Looks up CIP-68 metadata from a pre-fetched map. Falls back to DB query if not found.
     *
     * @param richFungibleToken whether the subject is a rich fungible token (label 444), see {@link #toFungibleTokenMetadata}
     */
    public Optional<FungibleTokenMetadata> findSubject(String policyId, String assetName, List<String> properties,
                                                       @Nullable Map<String, MetadataReferenceNft> prefetchedMap,
                                                       boolean richFungibleToken) {
        if (prefetchedMap != null) {
            MetadataReferenceNft referenceNft = prefetchedMap.get(policyId + ":" + assetName);
            if (referenceNft != null) {
                return Optional.of(toFungibleTokenMetadata(referenceNft, properties, richFungibleToken));
            }
            return Optional.empty();
        }
        return findSubject(policyId, assetName, properties, richFungibleToken);
    }

    /**
     * @param richFungibleToken whether the subject is a rich fungible token (label 444), see {@link #toFungibleTokenMetadata}
     */
    public Optional<FungibleTokenMetadata> findSubject(String policyId, String assetName, List<String> properties,
                                                       boolean richFungibleToken) {
        return metadataReferenceNftRepository.findFirstByPolicyIdAndAssetNameOrderBySlotDesc(policyId, assetName)
                .map(referenceNft -> toFungibleTokenMetadata(referenceNft, properties, richFungibleToken));
    }

    /**
     * The served metadata of a reference NFT for a subject, as indexed (ADR-019).
     * <ul>
     *   <li>A rich fungible token (444) has no {@code logo} in CIP-68: its {@code image} is served as the logo, as written,
     *       even when it is not a URI with an allowed scheme (its validation errors say so). A row indexed before the image
     *       was stored has none, so it is read from the stored datum.</li>
     *   <li>The validation errors are those of the subject's label and those that do not depend on a label: a reference
     *       NFT paired with a 222 and a 333 token is served as a 333 token without the 222 requirement (an image).</li>
     * </ul>
     */
    private FungibleTokenMetadata toFungibleTokenMetadata(MetadataReferenceNft referenceNft, List<String> properties,
                                                          boolean richFungibleToken) {
        String logo = richFungibleToken ? imageOf(referenceNft) : referenceNft.getLogo();
        int label = richFungibleToken ? LABEL_RFT : LABEL_FT;
        return new FungibleTokenMetadata(getPropertyIfRequired(Cip68FTDatumParser.DECIMALS, referenceNft.getDecimals(), properties),
                getPropertyIfRequired(Cip68FTDatumParser.DESCRIPTION, referenceNft.getDescription(), properties),
                getPropertyIfRequired(Cip68FTDatumParser.LOGO, logo, properties),
                getPropertyIfRequired(Cip68FTDatumParser.NAME, referenceNft.getName(), properties),
                getPropertyIfRequired(Cip68FTDatumParser.TICKER, referenceNft.getTicker(), properties),
                getPropertyIfRequired(Cip68FTDatumParser.URL, referenceNft.getUrl(), properties),
                getPropertyIfRequired(VERSION, referenceNft.getVersion(), properties),
                errorsForLabel(referenceNft.getValidationErrors(), label));
    }

    /** The errors of one label and those that do not depend on a label; {@code null} (never checked) stays {@code null}. */
    @Nullable
    static List<ValidationError> errorsForLabel(@Nullable List<ValidationError> errors, int label) {
        if (errors == null) {
            return null;
        }
        String subStandard = String.valueOf(label);
        return errors.stream()
                .filter(error -> error.subStandard() == null || subStandard.equals(error.subStandard()))
                .toList();
    }

    @Nullable
    private String imageOf(MetadataReferenceNft referenceNft) {
        if (referenceNft.getImage() != null) {
            return referenceNft.getImage();
        }
        AssetType referenceNftAssetType = new AssetType(referenceNft.getPolicyId(), referenceNft.getAssetName());
        return cip68DatumParser.parse(referenceNft.getDatum(), referenceNftAssetType)
                .map(ParsedCip68Datum::image)
                .orElse(null);
    }

    private <T> T getPropertyIfRequired(String propertyName, T propertyValue, List<String> properties) {
        if (properties.isEmpty() || properties.contains(propertyName)) {
            return propertyValue;
        } else {
            return null;
        }
    }


    /**
     * The reference NFT that holds the metadata of a served CIP-68 user token: a fungible token (label 333, prefix
     * {@code 0014df10}) or a rich fungible token (label 444, prefix {@code 001bc280}). Empty for any other subject.
     */
    public Optional<AssetType> getReferenceNftSubject(String subject) {
        AssetType assetType = AssetType.fromUnit(subject);
        String assetName = assetType.assetName();
        int tokenPrefixLength = REFERENCE_TOKEN_PREFIX.length();
        if (assetName.length() > tokenPrefixLength
                && (assetName.startsWith(FUNGIBLE_TOKEN_PREFIX) || assetName.startsWith(RICH_FUNGIBLE_TOKEN_PREFIX))) {
            String refNftAssetName = String.format("%s%s", REFERENCE_TOKEN_PREFIX, assetType.assetName().substring(tokenPrefixLength));
            return Optional.of(new AssetType(assetType.policyId(), refNftAssetName));
        } else {
            return Optional.empty();
        }
    }

    /** Whether the subject is a CIP-68 rich fungible token (label 444, prefix {@code 001bc280}). */
    public boolean isRichFungibleToken(String subject) {
        String assetName = AssetType.fromUnit(subject).assetName();
        return assetName.length() > RICH_FUNGIBLE_TOKEN_PREFIX.length() && assetName.startsWith(RICH_FUNGIBLE_TOKEN_PREFIX);
    }

}
