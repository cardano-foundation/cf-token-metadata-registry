package org.cardanofoundation.metadatatools.api;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import org.cardanofoundation.metadatatools.api.config.OffchainMetadataRegistryConfig;
import org.cardanofoundation.metadatatools.api.config.SpringWebSecurityConfig;
import org.cardanofoundation.metadatatools.api.controller.MetadataApiController;
import org.cardanofoundation.metadatatools.api.indexer.SimpleMetadataIndexer;
import org.cardanofoundation.metadatatools.api.model.rest.TokenMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.context.WebApplicationContext;

@WebMvcTest(MetadataApiController.class)
@Import(SpringWebSecurityConfig.class)
public class MetadataApiV1IntegrationTest {

  @Autowired private WebApplicationContext context;

  @Autowired private MockMvc mockMvc;

  @MockBean private SimpleMetadataIndexer simpleMetadataIndexer;

  @MockBean private OffchainMetadataRegistryConfig metadataRegistryConfig;

  @BeforeEach
  void setUp() {
    when(metadataRegistryConfig.networkIsMapped(anyString())).thenReturn(true);
    when(metadataRegistryConfig.sourceFromNetwork(anyString())).thenReturn("mainnet");
    when(simpleMetadataIndexer.findSubject("mainnet", "123456789abcde"))
        .thenReturn(Optional.empty());
    when(simpleMetadataIndexer.findSubject("mainnet", "123456789abcdef1"))
        .thenReturn(Optional.of(TokenMetadata.builder().subject("123456789abcdef1").build()));
  }

  @Test
  public void subjectQueryShouldReturnNoContentOnNonExistingSubject() throws Exception {
    mockMvc.perform(get("/metadata/123456789abcde")).andExpect(status().isNoContent());
  }

  @Test
  public void subjectQueryShouldReturnMetadata() throws Exception {
    mockMvc
        .perform(get("/metadata/123456789abcdef1"))
        .andExpect(status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$.subject").value("123456789abcdef1"));
  }
}
