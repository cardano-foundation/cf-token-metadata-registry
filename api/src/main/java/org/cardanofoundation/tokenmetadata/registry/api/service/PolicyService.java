package org.cardanofoundation.tokenmetadata.registry.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip113.ProgrammableTokenCip113;
import org.cardanofoundation.tokenmetadata.registry.api.model.v2.Extension;
import org.cardanofoundation.tokenmetadata.registry.api.model.v2.PolicyResponse;
import org.cardanofoundation.tokenmetadata.registry.api.model.v2.PolicyTokenSummary;
import org.cardanofoundation.tokenmetadata.registry.api.service.cip113.Cip113RegistryService;
import org.cardanofoundation.tokenmetadata.registry.entity.MetadataReferenceNft;
import org.cardanofoundation.tokenmetadata.registry.entity.TokenMetadata;
import org.cardanofoundation.tokenmetadata.registry.repository.MetadataReferenceNftRepository;
import org.cardanofoundation.tokenmetadata.registry.repository.TokenMetadataRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PolicyService {

    private final TokenMetadataRepository tokenMetadataRepository;
    private final MetadataReferenceNftRepository metadataReferenceNftRepository;
    private final Cip113RegistryService cip113RegistryService;

    /**
     * Look up a single policy: aggregates all CIP-26 and CIP-68 tokens plus extensions.
     */
    public Optional<PolicyResponse> findByPolicyId(String policyId) {
        List<PolicyTokenSummary> tokens = buildTokenSummaries(policyId);
        Map<String, Extension> extensions = buildExtensions(policyId);

        if (tokens.isEmpty() && extensions.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new PolicyResponse(policyId, tokens, extensions));
    }

    /**
     * Batch lookup of policies.
     */
    public List<PolicyResponse> findByPolicyIds(Collection<String> policyIds) {
        if (policyIds.isEmpty()) {
            return List.of();
        }

        List<String> policyIdList = List.copyOf(policyIds);
        Map<String, List<TokenMetadata>> cip26ByPolicy = tokenMetadataRepository.findByPolicyIn(policyIdList)
                .stream()
                .collect(Collectors.groupingBy(TokenMetadata::getPolicy));

        Map<String, List<MetadataReferenceNft>> cip68ByPolicy = metadataReferenceNftRepository.findLatestByPolicyIds(policyIdList)
                .stream()
                .collect(Collectors.groupingBy(MetadataReferenceNft::getPolicyId));

        Map<String, ProgrammableTokenCip113> cip113ByPolicy = cip113RegistryService.findByPolicyIds(policyIdList);

        List<PolicyResponse> results = new ArrayList<>();
        for (String policyId : policyIdList) {
            List<PolicyTokenSummary> tokens = mergeTokenSummaries(
                    cip26ByPolicy.getOrDefault(policyId, List.of()),
                    cip68ByPolicy.getOrDefault(policyId, List.of())
            );
            Map<String, Extension> extensions = buildExtensionsFromBatch(policyId, cip113ByPolicy);

            if (!tokens.isEmpty() || !extensions.isEmpty()) {
                results.add(new PolicyResponse(policyId, tokens, extensions));
            }
        }
        return results;
    }

    private Map<String, Extension> buildExtensions(String policyId) {
        Map<String, Extension> extensions = new LinkedHashMap<>();
        cip113RegistryService.findByPolicyId(policyId)
                .ifPresent(cip113 -> extensions.put(ProgrammableTokenCip113.EXTENSION_KEY, cip113));
        return extensions;
    }

    private Map<String, Extension> buildExtensionsFromBatch(String policyId,
                                                             Map<String, ProgrammableTokenCip113> cip113ByPolicy) {
        Map<String, Extension> extensions = new LinkedHashMap<>();
        ProgrammableTokenCip113 cip113 = cip113ByPolicy.get(policyId);
        if (cip113 != null) {
            extensions.put(ProgrammableTokenCip113.EXTENSION_KEY, cip113);
        }
        return extensions;
    }

    private List<PolicyTokenSummary> buildTokenSummaries(String policyId) {
        List<TokenMetadata> cip26Tokens = tokenMetadataRepository.findByPolicy(policyId);
        List<MetadataReferenceNft> cip68Tokens = metadataReferenceNftRepository.findLatestByPolicyIds(List.of(policyId));
        return mergeTokenSummaries(cip26Tokens, cip68Tokens);
    }

    /**
     * Merges CIP-26 and CIP-68 tokens into a deduplicated list of summaries.
     * CIP-68 takes precedence when the same subject exists in both.
     */
    private List<PolicyTokenSummary> mergeTokenSummaries(List<TokenMetadata> cip26Tokens,
                                                          List<MetadataReferenceNft> cip68Tokens) {
        Map<String, PolicyTokenSummary> bySubject = new LinkedHashMap<>();

        for (TokenMetadata token : cip26Tokens) {
            bySubject.put(token.getSubject(), new PolicyTokenSummary(
                    token.getSubject(),
                    token.getName(),
                    token.getTicker(),
                    token.getDecimals(),
                    "CIP_26"
            ));
        }

        for (MetadataReferenceNft nft : cip68Tokens) {
            String subject = nft.getPolicyId() + nft.getAssetName();
            bySubject.put(subject, new PolicyTokenSummary(
                    subject,
                    nft.getName(),
                    nft.getTicker(),
                    nft.getDecimals(),
                    "CIP_68"
            ));
        }

        return List.copyOf(bySubject.values());
    }

}
