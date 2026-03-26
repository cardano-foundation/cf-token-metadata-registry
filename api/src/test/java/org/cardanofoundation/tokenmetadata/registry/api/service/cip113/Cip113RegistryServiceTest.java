package org.cardanofoundation.tokenmetadata.registry.api.service.cip113;

import org.cardanofoundation.tokenmetadata.registry.api.config.Cip113Configuration;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip113.ProgrammableTokenCip113;
import org.cardanofoundation.tokenmetadata.registry.entity.Cip113RegistryNode;
import org.cardanofoundation.tokenmetadata.registry.repository.Cip113RegistryNodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Cip113RegistryServiceTest {

    @Mock
    private Cip113RegistryNodeRepository repository;

    private Cip113Configuration config;

    private Cip113RegistryService service;

    @BeforeEach
    void setUp() {
        config = new Cip113Configuration();
        config.setEnabled(true);
        config.setRegistryNftPolicyIds(List.of("abc123", "def456"));
        config.init();
        service = new Cip113RegistryService(repository, config);
    }

    @Test
    void findByPolicyIdReturnsDto() {
        Cip113RegistryNode entity = Cip113RegistryNode.builder()
                .policyId("deadbeef")
                .transferLogicScript("script1")
                .thirdPartyTransferLogicScript("script2")
                .globalStatePolicyId("globalState")
                .build();

        when(repository.findFirstByPolicyIdOrderBySlotDesc("deadbeef"))
                .thenReturn(Optional.of(entity));

        Optional<ProgrammableTokenCip113> result = service.findByPolicyId("deadbeef");

        assertTrue(result.isPresent());
        assertEquals("script1", result.get().transferLogicScript());
        assertEquals("script2", result.get().thirdPartyTransferLogicScript());
        assertEquals("globalState", result.get().globalStatePolicyId());
    }

    @Test
    void findByPolicyIdReturnsEmptyWhenNotFound() {
        when(repository.findFirstByPolicyIdOrderBySlotDesc("unknown"))
                .thenReturn(Optional.empty());

        assertTrue(service.findByPolicyId("unknown").isEmpty());
    }

    @Test
    void findByPolicyIdReturnsEmptyWhenDisabled() {
        config.setEnabled(false);
        assertTrue(service.findByPolicyId("deadbeef").isEmpty());
    }

    @Test
    void findByPolicyIdsBatchLookup() {
        Cip113RegistryNode entity1 = Cip113RegistryNode.builder()
                .policyId("policy1")
                .slot(100L)
                .transferLogicScript("s1")
                .thirdPartyTransferLogicScript("s2")
                .globalStatePolicyId("")
                .build();
        Cip113RegistryNode entity2 = Cip113RegistryNode.builder()
                .policyId("policy2")
                .slot(200L)
                .transferLogicScript("s3")
                .thirdPartyTransferLogicScript("s4")
                .globalStatePolicyId("gs")
                .build();

        when(repository.findLatestByPolicyIds(List.of("policy1", "policy2")))
                .thenReturn(List.of(entity1, entity2));

        Map<String, ProgrammableTokenCip113> result = service.findByPolicyIds(List.of("policy1", "policy2"));

        assertEquals(2, result.size());
        assertEquals("s1", result.get("policy1").transferLogicScript());
        assertEquals("s3", result.get("policy2").transferLogicScript());
    }

    @Test
    void findByPolicyIdsReturnsEmptyMapWhenDisabled() {
        config.setEnabled(false);
        assertTrue(service.findByPolicyIds(List.of("policy1")).isEmpty());
    }

}
