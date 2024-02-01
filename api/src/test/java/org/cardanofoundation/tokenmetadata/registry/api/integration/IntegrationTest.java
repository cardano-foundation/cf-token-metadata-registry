package org.cardanofoundation.tokenmetadata.registry.api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.SubjectsResponse;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.TokenMetadata;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.*;

@Slf4j
public class IntegrationTest {

    private static final String HASKEL_HOST = "tokens.cardano.org";

    private static final String TEMP_JAVA_HOST = "api.dev.cf-metadataserver-1.eu-central-1.metadata.testing.cf-deployments.org";

    private final RestTemplate restTemplate = new RestTemplate();

    private final ObjectMapper objectMapper = new ObjectMapper();


    @CsvSource({
            "4ffaa4ef3217df37c4995bb96066af4cb68dfcc66b9f2a10e0c333b95779726d73746f6e65",
            "07f019ce45fb638353258bda4316ce5eb3d0f76a3fb739c45174084953414c5459",
            "ff7cad970d3a755a1ff0335ccb3f3c1cabf31aacf3f23dd13db61b0630313030",
            "1d42b2025ed618de0d11c481a90438620e25a1c0e8b4ab737b401224546865547269706c6574734b54313846593139",
            "e4214b7cce62ac6fbba385d164df48e157eae5863521b4b67ca71d862c5ecc97a01efbfdbafe344a466fae6b23941bb750dafd8f84f6145635e96691",
            "3ea73755a53a11dd19297cb31281ff8f957c37e9b93c374f40403772506978656C43617264616E6F",
            "e4214b7cce62ac6fbba385d164df48e157eae5863521b4b67ca71d869400f7aaa73fb7b882a21c66c11ab806d3d197cbac239d257fdc3c0dd0cc278d",
            "e4214b7cce62ac6fbba385d164df48e157eae5863521b4b67ca71d86af33876a391b295fbed0e66dba561e3084f86f281bbd5eaa54d8e4232239a847",
            "4fde92c2f6dbcfa2879b44f7453872b31394cfb2f70f1d4c411169ac427562626c65",
            "5c4f08f47124b8e7ce9a4d0a00a5939da624cf6e533e1dc9de9b49c5556e636c6542656e6e793431",
            "e4214b7cce62ac6fbba385d164df48e157eae5863521b4b67ca71d86d4388a601aee88a1c70b9504645652af5879dc17af27c90d1a8f0738d5d8ac48",
            "320e3da27c6373e4bc045d7332c734ac393917755eb6b7689ce35ddb50524f5054454341",
            "e4214b7cce62ac6fbba385d164df48e157eae5863521b4b67ca71d868ea3529080de9d5784e4c3a44f7ed41ac6de20c38a39a8936515f74c8a0d1d14",
    })
    @ParameterizedTest
    @Disabled
    public void testFetchSubjects(String subject) throws IOException, InterruptedException {

        String javaResponse = restTemplate.getForObject("https://api.dev.cf-metadataserver-1.eu-central-1.metadata.testing.cf-deployments.org/mainnet/metadata/" + subject, String.class);
        log.info("java ok");

        String haskellResponse = restTemplate.getForObject("https://tokens.cardano.org/metadata/" + subject, String.class);
        log.info("haskel ok");

        var haskelSubject = objectMapper.readTree(haskellResponse);
        var javaSubject = objectMapper.readTree(javaResponse);
        if (javaSubject.isObject()) {
            ((ObjectNode) javaSubject).remove("additionalProperties");
        }

        Assertions.assertEquals(haskelSubject, javaSubject);

        Thread.sleep(1000L);

    }


    @CsvSource({
            "4ffaa4ef3217df37c4995bb96066af4cb68dfcc66b9f2a10e0c333b95779726d73746f6e65",
            "07f019ce45fb638353258bda4316ce5eb3d0f76a3fb739c45174084953414c5459",
            "ff7cad970d3a755a1ff0335ccb3f3c1cabf31aacf3f23dd13db61b0630313030",
            "1d42b2025ed618de0d11c481a90438620e25a1c0e8b4ab737b401224546865547269706c6574734b54313846593139",
            "e4214b7cce62ac6fbba385d164df48e157eae5863521b4b67ca71d862c5ecc97a01efbfdbafe344a466fae6b23941bb750dafd8f84f6145635e96691",
            "3ea73755a53a11dd19297cb31281ff8f957c37e9b93c374f40403772506978656C43617264616E6F",
            "e4214b7cce62ac6fbba385d164df48e157eae5863521b4b67ca71d869400f7aaa73fb7b882a21c66c11ab806d3d197cbac239d257fdc3c0dd0cc278d",
            "e4214b7cce62ac6fbba385d164df48e157eae5863521b4b67ca71d86af33876a391b295fbed0e66dba561e3084f86f281bbd5eaa54d8e4232239a847",
            "4fde92c2f6dbcfa2879b44f7453872b31394cfb2f70f1d4c411169ac427562626c65",
            "5c4f08f47124b8e7ce9a4d0a00a5939da624cf6e533e1dc9de9b49c5556e636c6542656e6e793431",
            "e4214b7cce62ac6fbba385d164df48e157eae5863521b4b67ca71d86d4388a601aee88a1c70b9504645652af5879dc17af27c90d1a8f0738d5d8ac48",
            "320e3da27c6373e4bc045d7332c734ac393917755eb6b7689ce35ddb50524f5054454341",
            "e4214b7cce62ac6fbba385d164df48e157eae5863521b4b67ca71d868ea3529080de9d5784e4c3a44f7ed41ac6de20c38a39a8936515f74c8a0d1d14",
    })

    @ParameterizedTest
    @Disabled
    public void fetchProps(String subject) throws Exception {

        List<String> properties = new ArrayList<>(List.of("name", "description", "url", "ticker", "decimals", "logo", "policy"));

        Collections.shuffle(properties);

        var property = properties.get(0);

        String javaResponse = restTemplate.getForObject(String.format("https://%s/mainnet/metadata/%s/properties/%s", TEMP_JAVA_HOST, subject, property), String.class);
        log.info("java ok");

        String haskellResponse = restTemplate.getForObject(String.format("https://%s/metadata/%s/properties/%s", HASKEL_HOST, subject, property), String.class);
        log.info("haskel ok");

        var haskelSubject = objectMapper.readValue(haskellResponse, TokenMetadata.class);
        var javaSubject = objectMapper.readValue(javaResponse, TokenMetadata.class);
        haskelSubject.setPolicy(javaSubject.getPolicy());

        Assertions.assertEquals(haskelSubject, javaSubject, String.format("Property %s", property));

        Thread.sleep(1000L);

    }

    private record Query(List<String> subjects, List<String> properties) {

    }

    @Test
    public void postSubjects() throws Exception {

        var subjects = List.of(
                "1d42b2025ed618de0d11c481a90438620e25a1c0e8b4ab737b401224546865547269706c6574734b54313846593139",
                "e4214b7cce62ac6fbba385d164df48e157eae5863521b4b67ca71d862c5ecc97a01efbfdbafe344a466fae6b23941bb750dafd8f84f6145635e96691",
                "3ea73755a53a11dd19297cb31281ff8f957c37e9b93c374f40403772506978656C43617264616E6F",
                "e4214b7cce62ac6fbba385d164df48e157eae5863521b4b67ca71d869400f7aaa73fb7b882a21c66c11ab806d3d197cbac239d257fdc3c0dd0cc278d",
                "e4214b7cce62ac6fbba385d164df48e157eae5863521b4b67ca71d86af33876a391b295fbed0e66dba561e3084f86f281bbd5eaa54d8e4232239a847",
                "4fde92c2f6dbcfa2879b44f7453872b31394cfb2f70f1d4c411169ac427562626c65",
                "5c4f08f47124b8e7ce9a4d0a00a5939da624cf6e533e1dc9de9b49c5556e636c6542656e6e793431"
        );

        List<String> allProperties = new ArrayList<>(List.of("name", "description", "url", "ticker", "decimals", "logo", "policy"));

        Collections.shuffle(allProperties);

        var properties = allProperties.subList(0, 3);

        var request = new Query(subjects, properties);

        String javaResponse = restTemplate.postForObject(String.format("https://%s/mainnet/metadata/query", TEMP_JAVA_HOST), request, String.class);
        log.info("java ok");

        String haskellResponse = restTemplate.postForObject(String.format("https://%s/metadata/query", HASKEL_HOST), request, String.class);
        log.info("haskel ok");

        var javaSubjects = objectMapper.readValue(javaResponse, SubjectsResponse.class);
        var haskelSubjects = objectMapper.readValue(haskellResponse, SubjectsResponse.class);

        javaSubjects.getSubjects().sort(Comparator.comparing(TokenMetadata::getSubject));
        haskelSubjects.getSubjects().sort(Comparator.comparing(TokenMetadata::getSubject));
        haskelSubjects.getSubjects().forEach(subject -> {
            var policy = findPolicy(subject.getSubject(), javaSubjects.getSubjects());
            subject.setPolicy(policy.orElse(null));
        });

        Assertions.assertEquals(haskelSubjects, javaSubjects, String.format("Property %s", String.join(", ", properties)));

    }

    public Optional<String> findPolicy(String subject, List<TokenMetadata> tokenMetadata) {
        return tokenMetadata.stream().filter(tm -> tm.getSubject().equalsIgnoreCase(subject)).findAny().map(TokenMetadata::getPolicy);
    }


}
