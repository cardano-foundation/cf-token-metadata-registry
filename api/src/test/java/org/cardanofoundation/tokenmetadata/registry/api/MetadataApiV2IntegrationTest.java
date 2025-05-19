package org.cardanofoundation.tokenmetadata.registry.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import net.minidev.json.JSONArray;
import org.cardanofoundation.tokenmetadata.registry.api.config.AppConfig;
import org.cardanofoundation.tokenmetadata.registry.api.config.SpringWebSecurityConfig;
import org.cardanofoundation.tokenmetadata.registry.api.controller.V2ApiController;
import org.cardanofoundation.tokenmetadata.registry.api.indexer.V1ApiMetadataIndexer;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.AnnotatedSignature;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.BatchRequest;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.TokenMetadata;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.wellknownproperties.NameProperty;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.wellknownproperties.TickerProperty;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.wellknownproperties.UrlProperty;
import org.cardanofoundation.tokenmetadata.registry.api.service.Cip68FungibleTokenService;
import org.cardanofoundation.tokenmetadata.registry.api.util.AssetType;
import org.cardanofoundation.tokenmetadata.registry.entity.MetadataReferenceNft;
import org.cardanofoundation.tokenmetadata.registry.repository.MetadataReferenceNftRepository;
import org.junit.jupiter.api.BeforeEach;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(V2ApiController.class)
@Import({SpringWebSecurityConfig.class, Cip68FungibleTokenService.class, AppConfig.CipPriorityConfiguration.class})
@ActiveProfiles("test")
public class MetadataApiV2IntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MetadataReferenceNftRepository metadataReferenceNftRepository;

    @MockBean
    private V1ApiMetadataIndexer v1ApiMetadataIndexer;

    @MockBean
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {

        var unknownAssetType = AssetType.fromUnit("025146866af908340247fe4e9672d5ac7059f1e8534696b5f920c9e66362544843");

        when(v1ApiMetadataIndexer.findSubject(unknownAssetType.toUnit()))
                .thenReturn(Optional.empty());

        when(metadataReferenceNftRepository.findByPolicyIdAndAssetName(unknownAssetType.policyId(), unknownAssetType.assetName()))
                .thenReturn(Optional.empty());

        final UrlProperty urlProperty = new UrlProperty();
        urlProperty.setSequenceNumber(BigDecimal.valueOf(1));
        urlProperty.setValue("https://fivebinaries.com/nutcoin");
        urlProperty.setSignatures(List.of(new AnnotatedSignature("1ff38761f6d93e58fd48e57c03cbeee848626a430f5d62b6cc555f7969b6636f07dbd0a7bf149cb577e95262c83efceb6bd0ba7724c2b146041d7853c75af603",
                "08c2ca6654c9e43b41b0b1560ee6a7bb4997629c2646575982934a51ecd71900")));


        // CIP 26 only token
        var knownAssetType = AssetType.fromUnit("025146866af908340247fe4e9672d5ac7059f1e8534696b5f920c9e66362544848");
        when(v1ApiMetadataIndexer.findSubjectSelectProperties(knownAssetType.toUnit(), List.of()))
                .thenReturn(Optional.of(TokenMetadata.builder()
                        .subject(knownAssetType.toUnit())
                        .url(urlProperty)
                        .build()));

        when(v1ApiMetadataIndexer.findSubjectSelectProperties(knownAssetType.toUnit(), List.of("url")))
                .thenReturn(Optional.of(TokenMetadata.builder()
                        .subject(knownAssetType.toUnit())
                        .url(urlProperty)
                        .build()));

        when(metadataReferenceNftRepository.findByPolicyIdAndAssetName(knownAssetType.policyId(), knownAssetType.assetName()))
                .thenReturn(Optional.of(MetadataReferenceNft.builder()
                        .policyId(knownAssetType.policyId())
                        .assetName(knownAssetType.assetName())
                        .name("NUTCOIN")
                        .url("https://cip68-url.com/nutcoin")
                        .build()));

        // CIP 26 and 68 token
        var fldtAssetType = AssetType.fromUnit("577f0b1342f8f8f4aed3388b80a8535812950c7a892495c0ecdf0f1e.0014df10464c4454");
        var ticker = new TickerProperty();
        ticker.setValue("FLDT");
        var name = new NameProperty();
        name.setValue("FLDT");
        var url = new UrlProperty();
        url.setValue("https://fluidtokens.com");
        when(v1ApiMetadataIndexer.findSubjectSelectProperties(fldtAssetType.toUnit(), List.of()))
                .thenReturn(Optional.of(TokenMetadata.builder()
                        .ticker(ticker)
                        .name(name)
                        .subject(fldtAssetType.toUnit())
                        .url(url)
                        .build()));

        when(metadataReferenceNftRepository.findByPolicyIdAndAssetName(fldtAssetType.policyId(), "000643b0464c4454"))
                .thenReturn(Optional.of(MetadataReferenceNft.builder()
                        .policyId(fldtAssetType.policyId())
                        .assetName(fldtAssetType.assetName())
                        .name("FLDT")
                        .ticker("FLDT")
                        .build()));

    }

    @Test
    public void subjectQueryShouldReturnNoContentOnNonExistingSubject() throws Exception {
        mockMvc.perform(get("/api/v2/subjects/025146866af908340247fe4e9672d5ac7059f1e8534696b5f920c9e66362544843"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void subjectQueryShouldReturnNoContentOnNonExistingSubjectOnTestnet() throws Exception {
        mockMvc.perform(get("/api/v2/subjects/025146866af908340247fe4e9672d5ac7059f1e8534696b5f920c9e66362544843"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void subjectQueryShouldReturnMetadata() throws Exception {
        mockMvc.perform(get("/api/v2/subjects/025146866af908340247fe4e9672d5ac7059f1e8534696b5f920c9e66362544848"))
                .andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$.subject.subject")
                        .value("025146866af908340247fe4e9672d5ac7059f1e8534696b5f920c9e66362544848"));
    }

    @Test
    public void subjectPropertyQueryShouldReturnNoContentOnNonExistingSubject() throws Exception {
        mockMvc.perform(get("/api/v2/subjects/025146866af908340247fe4e9672d5ac7059f1e8534696b5f920c9e66362544843"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void subjectPropertyQueryShouldReturnMetadata() throws Exception {
        List<Object> expectedArray = new JSONArray();
        expectedArray.add("CIP_68");
        expectedArray.add("CIP_26");
        mockMvc.perform(get("/api/v2/subjects/025146866af908340247fe4e9672d5ac7059f1e8534696b5f920c9e66362544848")
                        .queryParam("property", "url"))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.subject.subject").value("025146866af908340247fe4e9672d5ac7059f1e8534696b5f920c9e66362544848"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.subject.metadata.url.value").value("https://fivebinaries.com/nutcoin"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.queryPriority").value(expectedArray));
    }

    @Test
    public void cip68SubjectShouldReturnMetadata() throws Exception {
        List<Object> expectedArray = new JSONArray();
        expectedArray.add("CIP_68");
        expectedArray.add("CIP_26");
        mockMvc.perform(get("/api/v2/subjects/577f0b1342f8f8f4aed3388b80a8535812950c7a892495c0ecdf0f1e0014df10464c4454"))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.subject.subject").value("577f0b1342f8f8f4aed3388b80a8535812950c7a892495c0ecdf0f1e0014df10464c4454"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.subject.metadata.name.value").value("FLDT"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.subject.metadata.name.source").value("CIP_68"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.subject.metadata.url.value").value("https://fluidtokens.com"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.subject.metadata.url.source").value("CIP_26"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.queryPriority").value(expectedArray));
    }

    @Test
    public void cip68SubjectShouldReturnMetadataShowCipsDetails() throws Exception {
        mockMvc.perform(get("/api/v2/subjects/577f0b1342f8f8f4aed3388b80a8535812950c7a892495c0ecdf0f1e0014df10464c4454")
                        .queryParam("show_cips_details", "true"))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.subject.standards.cip26.name.value").value("FLDT"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.subject.standards.cip26.url.value").value("https://fluidtokens.com"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.subject.standards.cip68.name").value("FLDT"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.subject.standards.cip68.ticker").value("FLDT"))
        ;
    }

    @Test
    public void cip68SubjectShouldReturnMetadataWithCip26Prio() throws Exception {
        List<Object> expectedArray = new JSONArray();
        expectedArray.add("CIP_26");
        expectedArray.add("CIP_68");
        mockMvc.perform(get("/api/v2/subjects/577f0b1342f8f8f4aed3388b80a8535812950c7a892495c0ecdf0f1e0014df10464c4454")
                        .queryParam("query_priority", "CIP_26", "CIP_68"))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.subject.subject").value("577f0b1342f8f8f4aed3388b80a8535812950c7a892495c0ecdf0f1e0014df10464c4454"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.subject.metadata.name.value").value("FLDT"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.subject.metadata.name.source").value("CIP_26"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.subject.metadata.url.value").value("https://fluidtokens.com"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.subject.metadata.url.source").value("CIP_26"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.queryPriority").value(expectedArray));
    }

    @Test
    public void getSubjectsTest() throws Exception {

        var objectMapper = new ObjectMapper();

        var request = new BatchRequest(List.of("025146866af908340247fe4e9672d5ac7059f1e8534696b5f920c9e66362544848", "577f0b1342f8f8f4aed3388b80a8535812950c7a892495c0ecdf0f1e0014df10464c4454"), List.of());

        List<Object> expectedArray = new JSONArray();
        expectedArray.add("CIP_68");
        expectedArray.add("CIP_26");
        mockMvc.perform(post("/api/v2/subjects/query")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.subjects").isArray())
                // This checks the "length" of the subjects array is 2
                .andExpect(MockMvcResultMatchers.jsonPath("$.subjects.length()").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$.subjects[0].subject").value("025146866af908340247fe4e9672d5ac7059f1e8534696b5f920c9e66362544848"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.subjects[0].metadata.url.value").value("https://fivebinaries.com/nutcoin"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.subjects[1].subject").value("577f0b1342f8f8f4aed3388b80a8535812950c7a892495c0ecdf0f1e0014df10464c4454"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.subjects[1].metadata.name.value").value("FLDT"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.subjects[1].metadata.name.source").value("CIP_68"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.subjects[1].metadata.url.value").value("https://fluidtokens.com"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.subjects[1].metadata.url.source").value("CIP_26"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.queryPriority").value(expectedArray)
                );
    }


}
