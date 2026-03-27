package org.cardanofoundation.tokenmetadata.registry.api.service.cip113;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import org.cardanofoundation.tokenmetadata.registry.api.config.Cip113Configuration;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip113.Cip113RegistryEntry;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip113.ProgrammableTokenCip113;
import org.cardanofoundation.tokenmetadata.registry.entity.Cip113RegistryNode;
import org.cardanofoundation.tokenmetadata.registry.entity.MetadataReferenceNft;
import org.cardanofoundation.tokenmetadata.registry.repository.Cip113RegistryNodeRepository;
import org.cardanofoundation.tokenmetadata.registry.repository.MetadataReferenceNftRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Cip113RegistryService")
class Cip113RegistryServiceTest {

    private static final String MONITORED_POLICY = "aabbccdd11223344aabbccdd11223344aabbccdd11223344aabbccdd";
    private static final String OTHER_POLICY = "9999999999999999999999999999999999999999999999999999999999";

    @Mock
    private Cip113RegistryNodeRepository repository;

    @Mock
    private MetadataReferenceNftRepository metadataReferenceNftRepository;

    private Cip113Configuration config;
    private Cip113RegistryService service;

    @BeforeEach
    void setUp() {
        config = new Cip113Configuration();
        config.setRegistryNftPolicyIds(List.of(MONITORED_POLICY));
        config.init();
        service = new Cip113RegistryService(repository, metadataReferenceNftRepository, config);
    }

    @Nested
    @DisplayName("findByPolicyId")
    class FindByPolicyId {

        @Test
        void returnsDto() {
            Cip113RegistryNode entity = Cip113RegistryNode.builder()
                    .policyId("deadbeef")
                    .transferLogicScript("script1")
                    .thirdPartyTransferLogicScript("script2")
                    .globalStatePolicyId("globalState")
                    .build();

            when(repository.findFirstByPolicyIdOrderBySlotDesc("deadbeef"))
                    .thenReturn(Optional.of(entity));

            Optional<ProgrammableTokenCip113> result = service.findByPolicyId("deadbeef");

            assertThat(result).isPresent();
            assertThat(result.get().transferLogicScript()).isEqualTo("script1");
            assertThat(result.get().thirdPartyTransferLogicScript()).isEqualTo("script2");
            assertThat(result.get().globalStatePolicyId()).isEqualTo("globalState");
        }

        @Test
        void returnsEmptyWhenNotFound() {
            when(repository.findFirstByPolicyIdOrderBySlotDesc("unknown"))
                    .thenReturn(Optional.empty());

            assertThat(service.findByPolicyId("unknown")).isEmpty();
        }

        @Test
        void returnsEmptyWhenDisabled() {
            config.setRegistryNftPolicyIds(List.of());
            config.init();
            assertThat(service.findByPolicyId("deadbeef")).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByPolicyIds (batch)")
    class FindByPolicyIds {

        @Test
        void returnsMappedDtos() {
            Cip113RegistryNode entity1 = Cip113RegistryNode.builder()
                    .policyId("policy1").transferLogicScript("s1")
                    .thirdPartyTransferLogicScript("s2").globalStatePolicyId("").build();
            Cip113RegistryNode entity2 = Cip113RegistryNode.builder()
                    .policyId("policy2").transferLogicScript("s3")
                    .thirdPartyTransferLogicScript("s4").globalStatePolicyId("gs").build();

            when(repository.findLatestByPolicyIds(List.of("policy1", "policy2")))
                    .thenReturn(List.of(entity1, entity2));

            Map<String, ProgrammableTokenCip113> result = service.findByPolicyIds(List.of("policy1", "policy2"));

            assertThat(result).hasSize(2);
            assertThat(result.get("policy1").transferLogicScript()).isEqualTo("s1");
            assertThat(result.get("policy2").transferLogicScript()).isEqualTo("s3");
        }

        @Test
        void returnsEmptyMapWhenDisabled() {
            config.setRegistryNftPolicyIds(List.of());
            config.init();
            assertThat(service.findByPolicyIds(List.of("policy1"))).isEmpty();
        }

        @Test
        void returnsEmptyMapForEmptyInput() {
            assertThat(service.findByPolicyIds(List.of())).isEmpty();
        }
    }

    @Nested
    @DisplayName("containsRegistryNode")
    class ContainsRegistryNode {

        @Test
        void returnsTrueForMatchingNft() {
            AddressUtxo utxo = utxoWithAmount(MONITORED_POLICY + "deadbeef", BigInteger.ONE);
            assertThat(service.containsRegistryNode(utxo)).isTrue();
        }

        @Test
        void returnsFalseForNonMatchingPolicy() {
            AddressUtxo utxo = utxoWithAmount(OTHER_POLICY + "deadbeef", BigInteger.ONE);
            assertThat(service.containsRegistryNode(utxo)).isFalse();
        }

        @Test
        void returnsFalseWhenQuantityNotOne() {
            AddressUtxo utxo = utxoWithAmount(MONITORED_POLICY + "deadbeef", BigInteger.TEN);
            assertThat(service.containsRegistryNode(utxo)).isFalse();
        }

        @Test
        void returnsFalseWhenDisabled() {
            config.setRegistryNftPolicyIds(List.of());
            config.init();
            AddressUtxo utxo = utxoWithAmount(MONITORED_POLICY + "deadbeef", BigInteger.ONE);
            assertThat(service.containsRegistryNode(utxo)).isFalse();
        }

        private AddressUtxo utxoWithAmount(String unit, BigInteger quantity) {
            return AddressUtxo.builder()
                    .amounts(List.of(Amt.builder().unit(unit).quantity(quantity).build()))
                    .build();
        }
    }

    @Nested
    @DisplayName("findRegistryEntry")
    class FindRegistryEntry {

        @Test
        void returnsEnrichedEntryWhenCip68Exists() {
            Cip113RegistryNode node = Cip113RegistryNode.builder()
                    .policyId("deadbeef").transferLogicScript("s1")
                    .thirdPartyTransferLogicScript("s2").globalStatePolicyId(null).build();
            MetadataReferenceNft nft = MetadataReferenceNft.builder()
                    .policyId("deadbeef").name("MyToken").description("desc")
                    .ticker("MTK").decimals(6L).build();

            when(repository.findFirstByPolicyIdOrderBySlotDesc("deadbeef"))
                    .thenReturn(Optional.of(node));
            when(metadataReferenceNftRepository.findLatestByPolicyIds(List.of("deadbeef")))
                    .thenReturn(List.of(nft));

            Optional<Cip113RegistryEntry> result = service.findRegistryEntry("deadbeef");

            assertThat(result).isPresent();
            assertThat(result.get().policyId()).isEqualTo("deadbeef");
            assertThat(result.get().transferLogicScript()).isEqualTo("s1");
            assertThat(result.get().display()).isNotNull();
            assertThat(result.get().display().name()).isEqualTo("MyToken");
            assertThat(result.get().display().ticker()).isEqualTo("MTK");
        }

        @Test
        void returnsEntryWithNullDisplayWhenNoCip68() {
            Cip113RegistryNode node = Cip113RegistryNode.builder()
                    .policyId("deadbeef").transferLogicScript("s1")
                    .thirdPartyTransferLogicScript("s2").globalStatePolicyId(null).build();

            when(repository.findFirstByPolicyIdOrderBySlotDesc("deadbeef"))
                    .thenReturn(Optional.of(node));
            when(metadataReferenceNftRepository.findLatestByPolicyIds(List.of("deadbeef")))
                    .thenReturn(List.of());

            Optional<Cip113RegistryEntry> result = service.findRegistryEntry("deadbeef");

            assertThat(result).isPresent();
            assertThat(result.get().policyId()).isEqualTo("deadbeef");
            assertThat(result.get().display()).isNull();
        }

        @Test
        void returnsEmptyWhenNotFound() {
            when(repository.findFirstByPolicyIdOrderBySlotDesc("unknown"))
                    .thenReturn(Optional.empty());

            assertThat(service.findRegistryEntry("unknown")).isEmpty();
        }

        @Test
        void returnsEmptyWhenDisabled() {
            config.setRegistryNftPolicyIds(List.of());
            config.init();
            assertThat(service.findRegistryEntry("deadbeef")).isEmpty();
        }
    }

    @Nested
    @DisplayName("findRegistryEntries (batch)")
    class FindRegistryEntries {

        @Test
        void returnsEnrichedEntries() {
            Cip113RegistryNode node1 = Cip113RegistryNode.builder()
                    .policyId("p1").transferLogicScript("s1")
                    .thirdPartyTransferLogicScript("s2").build();
            Cip113RegistryNode node2 = Cip113RegistryNode.builder()
                    .policyId("p2").transferLogicScript("s3")
                    .thirdPartyTransferLogicScript("s4").build();

            when(repository.findLatestByPolicyIds(List.of("p1", "p2")))
                    .thenReturn(List.of(node1, node2));
            when(metadataReferenceNftRepository.findLatestByPolicyIds(List.of("p1", "p2")))
                    .thenReturn(List.of());

            List<Cip113RegistryEntry> result = service.findRegistryEntries(List.of("p1", "p2"));

            assertThat(result).hasSize(2);
            assertThat(result.get(0).policyId()).isEqualTo("p1");
            assertThat(result.get(0).display()).isNull();
            assertThat(result.get(1).policyId()).isEqualTo("p2");
        }

        @Test
        void returnsEmptyWhenDisabled() {
            config.setRegistryNftPolicyIds(List.of());
            config.init();
            assertThat(service.findRegistryEntries(List.of("p1"))).isEmpty();
        }

        @Test
        void returnsEmptyForEmptyInput() {
            assertThat(service.findRegistryEntries(List.of())).isEmpty();
        }
    }

}
