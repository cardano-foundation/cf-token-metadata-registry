package org.cardanofoundation.tokenmetadata.registry.api;

import jakarta.persistence.EntityManager;
import org.cardanofoundation.tokenmetadata.registry.api.config.SpringWebSecurityConfig;
import org.cardanofoundation.tokenmetadata.registry.api.controller.MetadataApiController;
import org.cardanofoundation.tokenmetadata.registry.api.indexer.V1ApiMetadataIndexer;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.AnnotatedSignature;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.TokenMetadata;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.wellknownproperties.UrlProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MetadataApiController.class)
@Import(SpringWebSecurityConfig.class)
@ActiveProfiles("test")
public class MetadataApiV1IntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private V1ApiMetadataIndexer v1ApiMetadataIndexer;

    @MockBean
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {

        when(v1ApiMetadataIndexer.findSubject("025146866af908340247fe4e9672d5ac7059f1e8534696b5f920c9e66362544843"))
                .thenReturn(Optional.empty());

        final UrlProperty urlProperty = new UrlProperty();
        urlProperty.setSequenceNumber(BigDecimal.valueOf(1));
        urlProperty.setValue("https://fivebinaries.com/nutcoin");
        urlProperty.setSignatures(List.of(new AnnotatedSignature("1ff38761f6d93e58fd48e57c03cbeee848626a430f5d62b6cc555f7969b6636f07dbd0a7bf149cb577e95262c83efceb6bd0ba7724c2b146041d7853c75af603",
                "08c2ca6654c9e43b41b0b1560ee6a7bb4997629c2646575982934a51ecd71900")));
        when(v1ApiMetadataIndexer.findSubject("025146866af908340247fe4e9672d5ac7059f1e8534696b5f920c9e66362544848"))
                .thenReturn(Optional.of(TokenMetadata.builder()
                        .subject("025146866af908340247fe4e9672d5ac7059f1e8534696b5f920c9e66362544848")
                        .url(urlProperty)
                        .build()));

        when(v1ApiMetadataIndexer.findSubjectSelectProperties(
                "025146866af908340247fe4e9672d5ac7059f1e8534696b5f920c9e66362544848",
                List.of("url")))
                .thenReturn(Optional.of(TokenMetadata.builder()
                        .subject("025146866af908340247fe4e9672d5ac7059f1e8534696b5f920c9e66362544848")
                        .url(urlProperty)
                        .build()));
    }

    @Test
    public void subjectQueryShouldReturnNoContentOnNonExistingSubject() throws Exception {
        mockMvc.perform(get("/metadata/025146866af908340247fe4e9672d5ac7059f1e8534696b5f920c9e66362544843"))
                .andExpect(status().isNoContent());
    }

    @Test
    public void subjectQueryShouldReturnNoContentOnNonExistingSubjectOnPreprod() throws Exception {
        mockMvc.perform(get("/metadata/025146866af908340247fe4e9672d5ac7059f1e8534696b5f920c9e66362544843"))
                .andExpect(status().isNoContent());
    }

    @Test
    @Disabled
    public void subjectQueryShouldReturnBadRequestOnNonMappedNetworkRequest() throws Exception {
        mockMvc.perform(get("/metadata/025146866af908340247fe4e9672d5ac7059f1e8534696b5f920c9e66362544843"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void subjectQueryShouldReturnMetadata() throws Exception {
        mockMvc.perform(get("/metadata/025146866af908340247fe4e9672d5ac7059f1e8534696b5f920c9e66362544848"))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.subject").value("025146866af908340247fe4e9672d5ac7059f1e8534696b5f920c9e66362544848"));
    }

    @Test
    public void subjectPropertyQueryShouldReturnNoContentOnNonExistingSubject() throws Exception {
        mockMvc.perform(get("/metadata/025146866af908340247fe4e9672d5ac7059f1e8534696b5f920c9e66362544843/properties/url"))
                .andExpect(status().isNoContent());
    }

    @Test
    public void subjectPropertyQueryShouldReturnMetadata() throws Exception {
        mockMvc.perform(get("/metadata/025146866af908340247fe4e9672d5ac7059f1e8534696b5f920c9e66362544848/properties/url"))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.subject").value("025146866af908340247fe4e9672d5ac7059f1e8534696b5f920c9e66362544848"));
    }
}
