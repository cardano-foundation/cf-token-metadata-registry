package org.cardanofoundation.tokenmetadata.registry.api.service;

import org.cardanofoundation.tokenmetadata.registry.api.model.cip113.ProgrammableTokenCip113;
import org.cardanofoundation.tokenmetadata.registry.api.model.v2.PolicyResponse;
import org.cardanofoundation.tokenmetadata.registry.api.service.cip113.Cip113RegistryService;
import org.cardanofoundation.tokenmetadata.registry.entity.MetadataReferenceNft;
import org.cardanofoundation.tokenmetadata.registry.entity.TokenMetadata;
import org.cardanofoundation.tokenmetadata.registry.repository.MetadataReferenceNftRepository;
import org.cardanofoundation.tokenmetadata.registry.repository.TokenMetadataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PolicyService")
class PolicyServiceTest {

    private static final String POLICY_ID = "ae563991eada7867dd5b734d7f0dbdbd7b8a26938b0256bba8cc77db";

    @Mock
    private TokenMetadataRepository tokenMetadataRepository;

    @Mock
    private MetadataReferenceNftRepository metadataReferenceNftRepository;

    @Mock
    private Cip113RegistryService cip113RegistryService;

    private PolicyService service;

    @BeforeEach
    void setUp() {
        service = new PolicyService(tokenMetadataRepository, metadataReferenceNftRepository, cip113RegistryService);
    }

    @Nested
    @DisplayName("findByPolicyId")
    class FindByPolicyId {

        @Test
        void returnsPolicyWithCip26Tokens() {
            TokenMetadata cip26 = new TokenMetadata();
            cip26.setSubject(POLICY_ID + "555344432");
            cip26.setPolicy(POLICY_ID);
            cip26.setName("USDC");
            cip26.setTicker("USDC");
            cip26.setDecimals(6L);

            when(tokenMetadataRepository.findByPolicy(POLICY_ID)).thenReturn(List.of(cip26));
            when(metadataReferenceNftRepository.findLatestByPolicyIds(List.of(POLICY_ID))).thenReturn(List.of());
            when(cip113RegistryService.findByPolicyId(POLICY_ID)).thenReturn(Optional.empty());

            Optional<PolicyResponse> result = service.findByPolicyId(POLICY_ID);

            assertThat(result).isPresent();
            assertThat(result.get().policyId()).isEqualTo(POLICY_ID);
            assertThat(result.get().tokens()).hasSize(1);
            assertThat(result.get().tokens().get(0).name()).isEqualTo("USDC");
            assertThat(result.get().tokens().get(0).source()).isEqualTo("CIP_26");
            assertThat(result.get().extensions()).isEmpty();
        }

        @Test
        void returnsPolicyWithCip68Tokens() {
            MetadataReferenceNft cip68 = MetadataReferenceNft.builder()
                    .policyId(POLICY_ID).assetName("0014df10555344432")
                    .name("USDC").ticker("USDC").decimals(6L).build();

            when(tokenMetadataRepository.findByPolicy(POLICY_ID)).thenReturn(List.of());
            when(metadataReferenceNftRepository.findLatestByPolicyIds(List.of(POLICY_ID))).thenReturn(List.of(cip68));
            when(cip113RegistryService.findByPolicyId(POLICY_ID)).thenReturn(Optional.empty());

            Optional<PolicyResponse> result = service.findByPolicyId(POLICY_ID);

            assertThat(result).isPresent();
            assertThat(result.get().tokens()).hasSize(1);
            assertThat(result.get().tokens().get(0).source()).isEqualTo("CIP_68");
        }

        @Test
        void returnsPolicyWithProgrammableOnly() {
            ProgrammableTokenCip113 cip113 = new ProgrammableTokenCip113("s1", "s2", null);

            when(tokenMetadataRepository.findByPolicy(POLICY_ID)).thenReturn(List.of());
            when(metadataReferenceNftRepository.findLatestByPolicyIds(List.of(POLICY_ID))).thenReturn(List.of());
            when(cip113RegistryService.findByPolicyId(POLICY_ID)).thenReturn(Optional.of(cip113));

            Optional<PolicyResponse> result = service.findByPolicyId(POLICY_ID);

            assertThat(result).isPresent();
            assertThat(result.get().tokens()).isEmpty();
            assertThat(result.get().extensions()).containsKey("cip113");
            ProgrammableTokenCip113 cip113Result = (ProgrammableTokenCip113) result.get().extensions().get("cip113");
            assertThat(cip113Result.transferLogicScript()).isEqualTo("s1");
        }

        @Test
        void returnsEmptyWhenNothingFound() {
            when(tokenMetadataRepository.findByPolicy(POLICY_ID)).thenReturn(List.of());
            when(metadataReferenceNftRepository.findLatestByPolicyIds(List.of(POLICY_ID))).thenReturn(List.of());
            when(cip113RegistryService.findByPolicyId(POLICY_ID)).thenReturn(Optional.empty());

            assertThat(service.findByPolicyId(POLICY_ID)).isEmpty();
        }

        @Test
        void cip68OverridesCip26ForSameSubject() {
            String subject = POLICY_ID + "0014df10555344432";

            TokenMetadata cip26 = new TokenMetadata();
            cip26.setSubject(subject);
            cip26.setPolicy(POLICY_ID);
            cip26.setName("OldName");
            cip26.setTicker("OLD");

            MetadataReferenceNft cip68 = MetadataReferenceNft.builder()
                    .policyId(POLICY_ID).assetName("0014df10555344432")
                    .name("NewName").ticker("NEW").decimals(6L).build();

            when(tokenMetadataRepository.findByPolicy(POLICY_ID)).thenReturn(List.of(cip26));
            when(metadataReferenceNftRepository.findLatestByPolicyIds(List.of(POLICY_ID))).thenReturn(List.of(cip68));
            when(cip113RegistryService.findByPolicyId(POLICY_ID)).thenReturn(Optional.empty());

            Optional<PolicyResponse> result = service.findByPolicyId(POLICY_ID);

            assertThat(result).isPresent();
            assertThat(result.get().tokens()).hasSize(1);
            assertThat(result.get().tokens().get(0).name()).isEqualTo("NewName");
            assertThat(result.get().tokens().get(0).source()).isEqualTo("CIP_68");
        }
    }

    @Nested
    @DisplayName("findByPolicyIds (batch)")
    class FindByPolicyIds {

        @Test
        void returnsPoliciesWithData() {
            String policy2 = "3c0eaea30e5e04c5c3f24049d06ebb470ee9442aaf4267d2474e5070";

            TokenMetadata cip26 = new TokenMetadata();
            cip26.setSubject(POLICY_ID + "aabb");
            cip26.setPolicy(POLICY_ID);
            cip26.setName("Token1");

            ProgrammableTokenCip113 cip113 = new ProgrammableTokenCip113("s1", "s2", null);

            when(tokenMetadataRepository.findByPolicyIn(List.of(POLICY_ID, policy2))).thenReturn(List.of(cip26));
            when(metadataReferenceNftRepository.findLatestByPolicyIds(List.of(POLICY_ID, policy2))).thenReturn(List.of());
            when(cip113RegistryService.findByPolicyIds(List.of(POLICY_ID, policy2)))
                    .thenReturn(Map.of(policy2, cip113));

            List<PolicyResponse> results = service.findByPolicyIds(List.of(POLICY_ID, policy2));

            assertThat(results).hasSize(2);
            assertThat(results.get(0).policyId()).isEqualTo(POLICY_ID);
            assertThat(results.get(0).tokens()).hasSize(1);
            assertThat(results.get(1).policyId()).isEqualTo(policy2);
            assertThat(results.get(1).extensions()).containsKey("cip113");
        }

        @Test
        void returnsEmptyForEmptyInput() {
            assertThat(service.findByPolicyIds(List.of())).isEmpty();
        }

        @Test
        void omitsUnknownPolicies() {
            when(tokenMetadataRepository.findByPolicyIn(List.of("unknown"))).thenReturn(List.of());
            when(metadataReferenceNftRepository.findLatestByPolicyIds(List.of("unknown"))).thenReturn(List.of());
            when(cip113RegistryService.findByPolicyIds(List.of("unknown"))).thenReturn(Map.of());

            assertThat(service.findByPolicyIds(List.of("unknown"))).isEmpty();
        }
    }

}
