package org.cardanofoundation.tokenmetadata.registry.api.controller;

import org.cardanofoundation.tokenmetadata.registry.api.config.SpringWebSecurityConfig;
import org.cardanofoundation.tokenmetadata.registry.api.config.ValidateEndpointConfig;
import org.cardanofoundation.tokenmetadata.registry.api.model.validate.ValidateResponse;
import org.cardanofoundation.tokenmetadata.registry.api.service.TokenValidationService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The HTTP side of the validate endpoint (ADR-023): its contract, the switch, the size bound and CORS. */
@WebMvcTest(ValidateApiController.class)
@Import({SpringWebSecurityConfig.class, ValidateEndpointConfig.class})
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "registry.validate.cors.allowed-origins=https://site.example",
        "registry.validate.max-request-bytes=1024"})
@SuppressWarnings("java:S5738") // @MockBean deprecated — kept for consistency with the other controller tests
class ValidateApiControllerTest {

    private static final String SITE = "https://site.example";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ValidateApiController controller;
    @MockBean
    private TokenValidationService tokenValidationService;
    @MockBean
    private EntityManager entityManager;

    @Test
    void answersWithWhatTheRegistryWouldDo() throws Exception {
        when(tokenValidationService.validateCip68(any())).thenReturn(
                new ValidateResponse("CIP-68", true, null, List.of(), List.of(), "d87980", null, List.of("note")));

        mockMvc.perform(post("/api/v2/validate/cip68").contentType(MediaType.APPLICATION_JSON).content("{\"datum\": \"d87980\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.standard").value("CIP-68"))
                .andExpect(jsonPath("$.stored").value(true))
                .andExpect(jsonPath("$.validation_errors").isArray())
                .andExpect(jsonPath("$.datum").value("d87980"));
    }

    @Test
    void aRequestItCannotAnswerIsABadRequest() throws Exception {
        when(tokenValidationService.validateCip68(any())).thenThrow(new IllegalArgumentException("give either datum (CBOR hex) or fields"));

        mockMvc.perform(post("/api/v2/validate/cip68").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("give either datum (CBOR hex) or fields"));
    }

    @Test
    void isNotFoundWhenSwitchedOff() throws Exception {
        ReflectionTestUtils.setField(controller, "enabled", false);
        try {
            mockMvc.perform(post("/api/v2/validate/cip26").contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isNotFound());
        } finally {
            ReflectionTestUtils.setField(controller, "enabled", true);
        }
    }

    @Test
    void refusesARequestLargerThanTheBound() throws Exception {
        String large = "{\"datum\": \"" + "a".repeat(2048) + "\"}";

        mockMvc.perform(post("/api/v2/validate/cip68").contentType(MediaType.APPLICATION_JSON).content(large))
                .andExpect(status().isPayloadTooLarge());
    }

    @Test
    void allowsTheConfiguredSiteToCallItFromABrowser() throws Exception {
        mockMvc.perform(options("/api/v2/validate/cip68")
                        .header(HttpHeaders.ORIGIN, SITE)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, SITE));
    }

    @Test
    void refusesOtherSites() throws Exception {
        mockMvc.perform(options("/api/v2/validate/cip68")
                        .header(HttpHeaders.ORIGIN, "https://elsewhere.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }
}
