package org.cardanofoundation.tokenmetadata.registry.api.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
public class Cip113Configuration {

    @Getter
    @Setter
    @Value("${cip113.enabled:false}")
    private boolean enabled;

    @Setter
    @Value("${cip113.registry.nft.policy-ids:}")
    private List<String> registryNftPolicyIds;

    @Getter
    private Set<String> registryNftPolicyIdSet;

    @PostConstruct
    public void init() {
        registryNftPolicyIdSet = registryNftPolicyIds.stream()
                .filter(id -> !id.isBlank())
                .collect(Collectors.toUnmodifiableSet());
        log.info("INIT - CIP-113 programmable tokens: enabled={}, registryNftPolicyIds={}",
                enabled, registryNftPolicyIdSet);
    }

    public boolean isMonitoredPolicyId(String policyId) {
        return registryNftPolicyIdSet.contains(policyId);
    }

}
