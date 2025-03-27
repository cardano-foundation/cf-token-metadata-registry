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
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class Cip68FungibleTokenService {

    // This represents the hex encoding of `(100)` the prefix in the name of the Reference Token
    private static final String REFERENCE_NFT_PREFIX = "000643b0";

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
     * @param utxo the utxo to check
     * @return true if any of the utxo's contains a Cip68 Reference NFT
     */
    public boolean containsReferenceNft(AddressUtxo utxo) {
        return utxo.getAmounts().stream().anyMatch(this::isReferenceNft);
    }

    /**
     * Checks and returns an NFT which matches Cip68 Reference NFT requirements if present
     * @param utxo the utxo to check
     * @return the amt matching the Referenct NFT if found
     */
    public Optional<Amt> extractReferenceNft(AddressUtxo utxo) {
        return utxo.getAmounts().stream().filter(this::isReferenceNft).findFirst();
    }

    /**
     * Check if the amount matches Cip68 Reference NFT Requirements
     * @param amount the amount to check
     * @return true if the amount is a Cip68 Reference NFT
     */
    public boolean isReferenceNft(Amt amount) {
        return amount.getQuantity().equals(BigInteger.ONE)
                && AssetType.fromUnit(amount.getUnit()).assetName().startsWith(REFERENCE_NFT_PREFIX);
    }

    public Optional<FungibleTokenMetadata> findSubject (String policyId, String assetName) {
        return metadataReferenceNftRepository.findByPolicyIdAndAssetNameV2(policyId,assetName)
                .map(referenceNft -> new FungibleTokenMetadata(referenceNft.getDecimals(),
                        referenceNft.getDescription(), referenceNft. getLogo(), referenceNft. getName(), referenceNft.getTicker(),
                        referenceNft. getUrl(), referenceNft.getVersion()));
    }


}
