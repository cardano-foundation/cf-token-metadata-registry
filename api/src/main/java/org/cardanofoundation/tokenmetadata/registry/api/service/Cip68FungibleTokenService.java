package org.cardanofoundation.tokenmetadata.registry.api.service;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip68.FungibleTokenMetadata;
import org.cardanofoundation.tokenmetadata.registry.api.util.AssetType;
import org.cardanofoundation.tokenmetadata.registry.repository.MetadataReferenceNftRepository;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

import static org.cardanofoundation.tokenmetadata.registry.api.model.cip68.Cip68Constants.FUNGIBLE_TOKEN_PREFIX;
import static org.cardanofoundation.tokenmetadata.registry.api.model.cip68.Cip68Constants.REFERENCE_TOKEN_PREFIX;

@Service
@RequiredArgsConstructor
@Slf4j
public class Cip68FungibleTokenService {

    // This represents the hex encoding of `(100)` the prefix in the name of the Reference Token
    private static final String REFERENCE_NFT_PREFIX = "000643b0";

    private static final String VERSION = "version";

    private final MetadataReferenceNftRepository metadataReferenceNftRepository;

    /**
     * In order to be a valid FT Token Metadata Reference datum there are some constraints (name and description must be present)
     *
     * @return true if the metadata are compliant to the FT Cip68 standard
     */
    public boolean isValidFTMetadata(FungibleTokenMetadata fungibleTokenMetadata) {
        return fungibleTokenMetadata.name() != null && fungibleTokenMetadata.description() != null;
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
     * Checks and returns an NFT which matches Cip68 Reference NFT requirements if present
     *
     * @param utxo the utxo to check
     * @return the amt matching the Referenct NFT if found
     */
    public Optional<Amt> extractReferenceNft(AddressUtxo utxo) {
        return utxo.getAmounts().stream().filter(this::isReferenceNft).findFirst();
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

    public Optional<FungibleTokenMetadata> findSubject(String policyId, String assetName, List<String> properties) {
        return metadataReferenceNftRepository.findByPolicyIdAndAssetName(policyId, assetName)
                .map(referenceNft -> new FungibleTokenMetadata(getPropertyIfRequired(Cip68FTDatumParser.DECIMALS, referenceNft.getDecimals(), properties),
                        getPropertyIfRequired(Cip68FTDatumParser.DESCRIPTION, referenceNft.getDescription(), properties),
                        getPropertyIfRequired(Cip68FTDatumParser.LOGO, referenceNft.getLogo(), properties),
                        getPropertyIfRequired(Cip68FTDatumParser.NAME, referenceNft.getName(), properties),
                        getPropertyIfRequired(Cip68FTDatumParser.TICKER, referenceNft.getTicker(), properties),
                        getPropertyIfRequired(Cip68FTDatumParser.URL, referenceNft.getUrl(), properties),
                        getPropertyIfRequired(VERSION, referenceNft.getVersion(), properties)));
    }

    private <T> T getPropertyIfRequired(String propertyName, T propertyValue, List<String> properties) {
        if (properties.isEmpty() || properties.contains(propertyName)) {
            return propertyValue;
        } else {
            return null;
        }
    }


    public Optional<AssetType> getReferenceNftSubject(String subject) {
        var assetType = AssetType.fromUnit(subject);
        var assetName = assetType.assetName();
        var tokenPrefixLength = REFERENCE_TOKEN_PREFIX.length();
        if (assetName.length() > tokenPrefixLength && assetName.startsWith(FUNGIBLE_TOKEN_PREFIX)) {
            var refNftAssetName = String.format("%s%s", REFERENCE_TOKEN_PREFIX, assetType.assetName().substring(tokenPrefixLength));
            return Optional.of(new AssetType(assetType.policyId(), refNftAssetName));
        } else {
            return Optional.empty();
        }
    }

}
