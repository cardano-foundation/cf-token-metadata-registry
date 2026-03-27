package org.cardanofoundation.tokenmetadata.registry.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.cardanofoundation.tokenmetadata.registry.api.config.SpringWebSecurityConfig;
import org.cardanofoundation.tokenmetadata.registry.api.controller.Cip113ApiController;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip113.*;
import org.cardanofoundation.tokenmetadata.registry.api.service.RegistryMetricsService;
import org.cardanofoundation.tokenmetadata.registry.api.service.cip113.Cip113RegistryService;
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
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(Cip113ApiController.class)
@Import(SpringWebSecurityConfig.class)
@ActiveProfiles("test")
@SuppressWarnings("java:S5738")
@DisplayName("Cip113ApiController")
class Cip113ApiControllerTest {

    private static final String POLICY_1 = "ae563991eada7867dd5b734d7f0dbdbd7b8a26938b0256bba8cc77db";
    private static final String POLICY_2 = "3c0eaea30e5e04c5c3f24049d06ebb470ee9442aaf4267d2474e5070";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private Cip113RegistryService cip113RegistryService;

    @MockBean
    private RegistryMetricsService metricsService;

    @MockBean
    private EntityManager entityManager;

    private static Cip113RegistryEntry entryWithDisplay(String policyId) {
        return new Cip113RegistryEntry(policyId, "script1", "script2", null,
                new Cip113DisplayMetadata("TestToken", "A test token", "TST", 6L, null));
    }

    private static Cip113RegistryEntry entryWithoutDisplay(String policyId) {
        return new Cip113RegistryEntry(policyId, "script1", "script2", "globalState", null);
    }

    @Nested
    @DisplayName("GET /api/v2/cip113/registry/{policyId}")
    class GetRegistryEntry {

        @Test
        void returnsEntryWhenFound() throws Exception {
            when(cip113RegistryService.findRegistryEntry(POLICY_1))
                    .thenReturn(Optional.of(entryWithDisplay(POLICY_1)));

            mockMvc.perform(get("/api/v2/cip113/registry/{policyId}", POLICY_1))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.policy_id").value(POLICY_1))
                    .andExpect(jsonPath("$.transfer_logic_script").value("script1"))
                    .andExpect(jsonPath("$.third_party_transfer_logic_script").value("script2"))
                    .andExpect(jsonPath("$.display.name").value("TestToken"))
                    .andExpect(jsonPath("$.display.ticker").value("TST"));
        }

        @Test
        void returns404WhenNotFound() throws Exception {
            when(cip113RegistryService.findRegistryEntry("unknown"))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v2/cip113/registry/unknown"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void displayIsNullWhenNoCip68Data() throws Exception {
            when(cip113RegistryService.findRegistryEntry(POLICY_1))
                    .thenReturn(Optional.of(entryWithoutDisplay(POLICY_1)));

            mockMvc.perform(get("/api/v2/cip113/registry/{policyId}", POLICY_1))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.policy_id").value(POLICY_1))
                    .andExpect(jsonPath("$.global_state_policy_id").value("globalState"))
                    .andExpect(jsonPath("$.display").doesNotExist());
        }
    }

    @Nested
    @DisplayName("POST /api/v2/cip113/registry/query")
    class QueryRegistry {

        @Test
        void returnsMatchingEntries() throws Exception {
            when(cip113RegistryService.findRegistryEntries(List.of(POLICY_1, POLICY_2)))
                    .thenReturn(List.of(entryWithDisplay(POLICY_1), entryWithoutDisplay(POLICY_2)));

            Cip113BatchRequest request = new Cip113BatchRequest(List.of(POLICY_1, POLICY_2));

            mockMvc.perform(post("/api/v2/cip113/registry/query")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].policy_id").value(POLICY_1))
                    .andExpect(jsonPath("$[1].policy_id").value(POLICY_2));
        }

        @Test
        void returnsEmptyListForUnknownPolicies() throws Exception {
            when(cip113RegistryService.findRegistryEntries(List.of("unknown")))
                    .thenReturn(List.of());

            Cip113BatchRequest request = new Cip113BatchRequest(List.of("unknown"));

            mockMvc.perform(post("/api/v2/cip113/registry/query")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

}
