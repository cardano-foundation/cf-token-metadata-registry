package org.cardanofoundation.tokenmetadata.registry.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.api.model.v2.PolicyBatchRequest;
import org.cardanofoundation.tokenmetadata.registry.api.model.v2.PolicyResponse;
import org.cardanofoundation.tokenmetadata.registry.api.service.PolicyService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/v2")
@Slf4j
public class PolicyApiController implements PolicyApi {

    private final PolicyService policyService;

    @Override
    public ResponseEntity<PolicyResponse> getPolicy(String policyId) {
        return policyService.findByPolicyId(policyId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<List<PolicyResponse>> queryPolicies(PolicyBatchRequest body) {
        List<PolicyResponse> results = policyService.findByPolicyIds(body.policyIds());
        return ResponseEntity.ok(results);
    }

}
