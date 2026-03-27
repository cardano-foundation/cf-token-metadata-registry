package org.cardanofoundation.tokenmetadata.registry.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.cardanofoundation.tokenmetadata.registry.api.config.SpringWebSecurityConfig;
import org.cardanofoundation.tokenmetadata.registry.api.controller.PolicyApiController;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip113.ProgrammableTokenCip113;
import org.cardanofoundation.tokenmetadata.registry.api.model.v2.PolicyBatchRequest;
import org.cardanofoundation.tokenmetadata.registry.api.model.v2.PolicyResponse;
import org.cardanofoundation.tokenmetadata.registry.api.model.v2.PolicyTokenSummary;
import org.cardanofoundation.tokenmetadata.registry.api.service.PolicyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PolicyApiController.class)
@Import(SpringWebSecurityConfig.class)
@ActiveProfiles("test")
@SuppressWarnings("java:S5738")
@DisplayName("PolicyApiController")
class PolicyApiControllerTest {

    private static final String POLICY_ID = "ae563991eada7867dd5b734d7f0dbdbd7b8a26938b0256bba8cc77db";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PolicyService policyService;

    @MockBean
    private EntityManager entityManager;

    @Nested
    @DisplayName("GET /api/v2/policies/{policyId}")
    class GetPolicy {

        @Test
        void returnsFullPolicyResponse() throws Exception {
            PolicyTokenSummary token = new PolicyTokenSummary(POLICY_ID + "555344432", "USDC", "USDC", 6L, "CIP_68");
            ProgrammableTokenCip113 cip113 = new ProgrammableTokenCip113("s1", "s2", null);
            PolicyResponse response = new PolicyResponse(POLICY_ID, List.of(token),
                    Map.of(ProgrammableTokenCip113.EXTENSION_KEY, cip113));

            when(policyService.findByPolicyId(POLICY_ID)).thenReturn(Optional.of(response));

            mockMvc.perform(get("/api/v2/policies/{policyId}", POLICY_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.policy_id").value(POLICY_ID))
                    .andExpect(jsonPath("$.tokens[0].name").value("USDC"))
                    .andExpect(jsonPath("$.tokens[0].source").value("CIP_68"))
                    .andExpect(jsonPath("$.extensions.cip113.transfer_logic_script").value("s1"));
        }

        @Test
        void returns404WhenNotFound() throws Exception {
            when(policyService.findByPolicyId("unknown")).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v2/policies/unknown"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void extensionsOmittedWhenEmpty() throws Exception {
            PolicyTokenSummary token = new PolicyTokenSummary(POLICY_ID + "aabb", "Token", "TKN", null, "CIP_26");
            PolicyResponse response = new PolicyResponse(POLICY_ID, List.of(token), Map.of());

            when(policyService.findByPolicyId(POLICY_ID)).thenReturn(Optional.of(response));

            mockMvc.perform(get("/api/v2/policies/{policyId}", POLICY_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tokens[0].name").value("Token"))
                    .andExpect(jsonPath("$.extensions").doesNotExist());
        }
    }

    @Nested
    @DisplayName("POST /api/v2/policies/query")
    class QueryPolicies {

        @Test
        void returnsMatchingPolicies() throws Exception {
            ProgrammableTokenCip113 cip113 = new ProgrammableTokenCip113("s1", "s2", null);
            PolicyResponse response = new PolicyResponse(POLICY_ID, List.of(),
                    Map.of(ProgrammableTokenCip113.EXTENSION_KEY, cip113));

            when(policyService.findByPolicyIds(List.of(POLICY_ID))).thenReturn(List.of(response));

            PolicyBatchRequest request = new PolicyBatchRequest(List.of(POLICY_ID));

            mockMvc.perform(post("/api/v2/policies/query")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].policy_id").value(POLICY_ID))
                    .andExpect(jsonPath("$[0].extensions.cip113.transfer_logic_script").value("s1"));
        }

        @Test
        void returnsEmptyForUnknownPolicies() throws Exception {
            when(policyService.findByPolicyIds(List.of("unknown"))).thenReturn(List.of());

            PolicyBatchRequest request = new PolicyBatchRequest(List.of("unknown"));

            mockMvc.perform(post("/api/v2/policies/query")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

}
