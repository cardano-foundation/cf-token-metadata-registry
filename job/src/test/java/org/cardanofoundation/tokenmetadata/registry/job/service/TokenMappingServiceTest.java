package org.cardanofoundation.tokenmetadata.registry.job.service;

import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.job.model.Item;
import org.cardanofoundation.tokenmetadata.registry.job.model.Mapping;
import org.cardanofoundation.tokenmetadata.registry.job.model.Signature;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.util.ResourceUtils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

@Slf4j
public class TokenMappingServiceTest {

    private final TokenMappingService tokenMappingService = new TokenMappingService();

    @CsvSource({
            "4ffaa4ef3217df37c4995bb96066af4cb68dfcc66b9f2a10e0c333b95779726d73746f6e65.json",
            "07f019ce45fb638353258bda4316ce5eb3d0f76a3fb739c45174084953414c5459.json",
            "ff7cad970d3a755a1ff0335ccb3f3c1cabf31aacf3f23dd13db61b0630313030.json",
    })
    @ParameterizedTest
    public void deserializeOk(String mappingFileName) throws FileNotFoundException {
        var mappingFile = ResourceUtils.getFile(String.format("classpath:mappings/%s", mappingFileName));
        var mappingsOpt = tokenMappingService.parseMappings(mappingFile);
        Assertions.assertTrue(mappingsOpt.isPresent(), "Mappings are supposed to be present");
    }


    @Test
    public void mappingsAreCorrectlyDeserialized() throws IOException {
        var mappingFile = ResourceUtils.getFile("classpath:mappings/4ffaa4ef3217df37c4995bb96066af4cb68dfcc66b9f2a10e0c333b95779726d73746f6e65.json");
        var logoFile = ResourceUtils.getFile("classpath:mappings/4ffaa4ef3217df37c4995bb96066af4cb68dfcc66b9f2a10e0c333b95779726d73746f6e65-logo.txt");
        var mappingsOpt = tokenMappingService.parseMappings(mappingFile);

        var logo = new BufferedReader(new FileReader(logoFile)).readLine();

        final var expected = new Mapping("4ffaa4ef3217df37c4995bb96066af4cb68dfcc66b9f2a10e0c333b95779726d73746f6e65",
                // url
                new Item(0, "https://tavernsquad.io", List.of(new Signature("6da949217f93874f2d6bda6e2af83ecdde0b5425fa778c75c5feb2800fd201132f4825f0f199e5786e00a3268afe44f9ef2213d14a4872f685e37aef68637106", "dff641503af0d00b672967f88b9a6f448588d48838473c8a4c73930b55d0fb73"))),
                // name
                new Item(0, "Wyrmstone", List.of(new Signature("bc019813aa1f356e60bec3e311130252bd6edaf6d1e47fe6051fe4511e3fe3d0db3d3389b5e5ee75f53890be354267886e9f750b8254be73d9efcc4b6e68f409", "dff641503af0d00b672967f88b9a6f448588d48838473c8a4c73930b55d0fb73"))),
                // ticker
                new Item(0, "WYRM", List.of(new Signature("d7c850ae1e9669a647fe3e8f8fc1a3937ae1bdde4fc94e44974ae4735d1b8765ebf9081278eaff9f492bd3d79dda4f4abd0e3d30fea9dbb9c8463e909f7ec60c", "dff641503af0d00b672967f88b9a6f448588d48838473c8a4c73930b55d0fb73"))),
                // decimals
                new Item(0, "6", List.of(new Signature("c706b0ac733946436ece2ddf7fb47eeab471e7a594bff02fa9a4789214f45056a531a215ee9d8157b40766b91eab79d03b992bb5f8ab32203ac05c933900a608", "dff641503af0d00b672967f88b9a6f448588d48838473c8a4c73930b55d0fb73"))),
                // logo
                new Item(0, logo, List.of(new Signature("bb0b7d7b33457821e5f322367fb116a811e40de3a2adbd5face9896e3eed54da29050b103e34691ddd72340bcf2b07332964635e46b384594f4d37bb59d1e303", "dff641503af0d00b672967f88b9a6f448588d48838473c8a4c73930b55d0fb73"))),
                "82008200581ce943575bd64841810b1110f1801572fd8b7ec15a57ca49e15e2690c7",
                // description
                new Item(0, "The official utility token of Tavern Squad", List.of(new Signature("c73ec3c9a0e3a61b8a41e55eff49d89b7b889a77a4002c75f561b5b7df4a9671141d637d0e501aa320c48ab307df2ff5130d225e87a167e3e43a568a1de8d709", "dff641503af0d00b672967f88b9a6f448588d48838473c8a4c73930b55d0fb73"))));

        Assertions.assertTrue(mappingsOpt.isPresent());

        mappingsOpt.ifPresent(mappings -> Assertions.assertEquals(expected, mappings));


    }

    @Test
    public void mappingsAreCorrectlyDeserialized_2() throws IOException {
        var mappingFile = ResourceUtils.getFile("classpath:mappings/ff7cad970d3a755a1ff0335ccb3f3c1cabf31aacf3f23dd13db61b0630313030.json");
        var logoFile = ResourceUtils.getFile("classpath:mappings/ff7cad970d3a755a1ff0335ccb3f3c1cabf31aacf3f23dd13db61b0630313030-logo.txt");
        var mappingsOpt = tokenMappingService.parseMappings(mappingFile);

        var logo = new BufferedReader(new FileReader(logoFile)).readLine();

        final var expected = new Mapping("ff7cad970d3a755a1ff0335ccb3f3c1cabf31aacf3f23dd13db61b0630313030",
                // url
                new Item(0, "https://adage.app/nft-giveaway", List.of(new Signature("4e38c8a7b47445e1471a7701395d5c7ae410c4c4f310d61c1d697b7672162b1dcb5e224c01ed74c6708501be6711c9f6f8ad3d0ae50cd4d2be81656fe364f706", "16ee09e977811d32dd7aab7decd223307e0037abd85a1374ae838648c72da7b9"))),
                // name
                new Item(0, "Spirit Of The Bone Forest", List.of(new Signature("9b4d04103423bcf8587867197d33a72b15311a773c8f2977d38e0f84a20ec5e219cf3d63afe08d017150280bcdcfc84b3077d04bc56e5fb593f61d0cb6922b01", "16ee09e977811d32dd7aab7decd223307e0037abd85a1374ae838648c72da7b9"))),
                // ticker
                new Item(0, "0100", List.of(new Signature("71b8841fde20124e19ee5e2bc824eb4bcf1aa1c598182c66bf3a1811bdf625ce1bc9b898a8bfd7ebf6364034ba8d604178c8a4e0a684d20ac1b59f128d3d8106", "16ee09e977811d32dd7aab7decd223307e0037abd85a1374ae838648c72da7b9"))),
                // decimals
                null,
                // logo
                new Item(0, logo, List.of(new Signature("0bd2c4bba09fd54f0f67e83d6e818c3c1474173e0aeba9feaac681ca92abeb625397e2e9e7c2c7f25924107666d1a15ea21ca8f33bab752f568c55e7da12a608", "16ee09e977811d32dd7aab7decd223307e0037abd85a1374ae838648c72da7b9"))),
                "820182018282051a02f893828200581cdc1ac66efbf0f27457cd646d80fbeee08eafcacce42fb631ca1a0254",
                // description
                new Item(0, "Part of the ADAGE NFT giveaway and Veritree donation", List.of(new Signature("867a112704870f16ccee3be3290ff46fdadc4904ab6a01cebe304adf42e120511fb53b23d2e5032d051be40c48941350da7f37fe465df1306b2796a532c97801", "16ee09e977811d32dd7aab7decd223307e0037abd85a1374ae838648c72da7b9"))));

        Assertions.assertTrue(mappingsOpt.isPresent());

        mappingsOpt.ifPresent(mappings -> Assertions.assertEquals(expected, mappings));


    }


}
