package org.cardanofoundation.tokenmetadata.registry.api.service;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip68.Cip68Uri;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip68.FungibleTokenMetadata;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip68.ParsedCip68Datum;
import org.cardanofoundation.tokenmetadata.registry.api.util.AssetType;
import org.cardanofoundation.tokenmetadata.registry.repository.MetadataReferenceNftRepository;
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
import static org.cardanofoundation.tokenmetadata.registry.api.model.cip68.Cip68Constants.REFERENCE_TOKEN_PREFIX;

@Service
@RequiredArgsConstructor
@Slf4j
public class Cip68FungibleTokenService {

    // This represents the hex encoding of `(100)` the prefix in the name of the Reference Token
    private static final String REFERENCE_NFT_PREFIX = "000643b0";

    private static final String VERSION = "version";
    private static final int LOGGED_URI_MAX_LENGTH = 60;

    private final MetadataReferenceNftRepository metadataReferenceNftRepository;

    /**
     * Says why a CIP-68 datum is not valid for a user-token label, or an empty result when it is. The check is strict
     * about what CIP-68 requires for the label:
     * <ul>
     *   <li>{@code name} for every label;</li>
     *   <li>for the 333 fungible token, {@code description};</li>
     *   <li>for the 222 NFT and the 444 RFT, {@code image} as a URI with the scheme {@code https}, {@code ipfs},
     *       {@code ar} or {@code data} ({@code description} is optional for them).</li>
     * </ul>
     * The optional fields do not reject the datum: see {@link #invalidLogoReason}.
     *
     * @param datum the parsed datum
     * @param label the user-token label the datum belongs to: {@code 222}, {@code 333} or {@code 444}
     * @return the reason the datum is not valid for the label, empty if it is valid
     */
    public Optional<String> invalidReason(ParsedCip68Datum datum, int label) {
        if (datum.name() == null) {
            return Optional.of("it has no name");
        }
        if (label == LABEL_FT) {
            if (datum.description() == null) {
                return Optional.of("it has no description, which CIP-68 requires for a fungible token (label "
                        + LABEL_FT + ")");
            }
            return Optional.empty();
        }
        String image = datum.image();
        if (image == null || image.isBlank()) {
            return Optional.of("it has no image, which CIP-68 requires for label " + label);
        }
        if (!Cip68Uri.hasAllowedScheme(image)) {
            return Optional.of("its image '" + abbreviate(image) + "' is not a URI with one of the schemes CIP-68 "
                    + "allows (" + Cip68Uri.ALLOWED_SCHEMES + ")");
        }
        return Optional.empty();
    }

    /**
     * Says why the {@code logo} of a datum cannot be stored, or an empty result when there is no logo or it is fine.
     * The logo is optional, so a bad one does not reject the datum: it is left out and the rest is indexed. CIP-68
     * defines it as a URI whose scheme is one of {@code https}, {@code ipfs}, {@code ar} or {@code data}, so a bare
     * IPFS hash, or any other scheme, is not accepted.
     *
     * @param datum the parsed datum
     * @return the reason the logo is left out, empty if there is none or it is valid
     */
    public Optional<String> invalidLogoReason(ParsedCip68Datum datum) {
        String logo = datum.logo();
        if (logo == null || logo.isBlank() || Cip68Uri.hasAllowedScheme(logo)) {
            return Optional.empty();
        }
        return Optional.of("its logo '" + abbreviate(logo) + "' is not a URI with one of the schemes CIP-68 allows ("
                + Cip68Uri.ALLOWED_SCHEMES + ")");
    }

    private static String abbreviate(String value) {
        String oneLine = value.replaceAll("\\s+", " ");
        return oneLine.length() <= LOGGED_URI_MAX_LENGTH ? oneLine : oneLine.substring(0, LOGGED_URI_MAX_LENGTH) + "...";
    }

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
     */
    public Optional<FungibleTokenMetadata> findSubject(String policyId, String assetName, List<String> properties,
                                                       @Nullable Map<String, MetadataReferenceNft> prefetchedMap) {
        if (prefetchedMap != null) {
            MetadataReferenceNft referenceNft = prefetchedMap.get(policyId + ":" + assetName);
            if (referenceNft != null) {
                return Optional.of(toFungibleTokenMetadata(referenceNft, properties));
            }
            return Optional.empty();
        }
        return findSubject(policyId, assetName, properties);
    }

    public Optional<FungibleTokenMetadata> findSubject(String policyId, String assetName, List<String> properties) {
        return metadataReferenceNftRepository.findFirstByPolicyIdAndAssetNameOrderBySlotDesc(policyId, assetName)
                .map(referenceNft -> toFungibleTokenMetadata(referenceNft, properties));
    }

    private FungibleTokenMetadata toFungibleTokenMetadata(MetadataReferenceNft referenceNft, List<String> properties) {
        return new FungibleTokenMetadata(getPropertyIfRequired(Cip68FTDatumParser.DECIMALS, referenceNft.getDecimals(), properties),
                getPropertyIfRequired(Cip68FTDatumParser.DESCRIPTION, referenceNft.getDescription(), properties),
                getPropertyIfRequired(Cip68FTDatumParser.LOGO, referenceNft.getLogo(), properties),
                getPropertyIfRequired(Cip68FTDatumParser.NAME, referenceNft.getName(), properties),
                getPropertyIfRequired(Cip68FTDatumParser.TICKER, referenceNft.getTicker(), properties),
                getPropertyIfRequired(Cip68FTDatumParser.URL, referenceNft.getUrl(), properties),
                getPropertyIfRequired(VERSION, referenceNft.getVersion(), properties));
    }

    private <T> T getPropertyIfRequired(String propertyName, T propertyValue, List<String> properties) {
        if (properties.isEmpty() || properties.contains(propertyName)) {
            return propertyValue;
        } else {
            return null;
        }
    }


    public Optional<AssetType> getReferenceNftSubject(String subject) {
        AssetType assetType = AssetType.fromUnit(subject);
        String assetName = assetType.assetName();
        int tokenPrefixLength = REFERENCE_TOKEN_PREFIX.length();
        if (assetName.length() > tokenPrefixLength && assetName.startsWith(FUNGIBLE_TOKEN_PREFIX)) {
            String refNftAssetName = String.format("%s%s", REFERENCE_TOKEN_PREFIX, assetType.assetName().substring(tokenPrefixLength));
            return Optional.of(new AssetType(assetType.policyId(), refNftAssetName));
        } else {
            return Optional.empty();
        }
    }

}
