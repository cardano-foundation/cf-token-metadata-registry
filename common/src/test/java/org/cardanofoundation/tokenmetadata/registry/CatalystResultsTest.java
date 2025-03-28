package org.cardanofoundation.tokenmetadata.registry;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class CatalystResultsTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record Contribution(String stakePublicKey, String rewardAddress, Long value) {

    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record Hir(String votingKey, String address, String votingGroup, Long votingPower, Boolean underthreshold) {

    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record Voter(List<Contribution> contributions, Hir hir) {

    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Voters(List<Voter> voters) {

    }


    @Test
    public void readRecords() throws IOException {

        var registeredVotersFile = new File("/Users/giovanni/Development/workspace/catalyst-core/cexplorer-140572806.final.json");

        var voters = OBJECT_MAPPER.readValue(registeredVotersFile, Voters.class);

        log.info("voters size: {}", voters.voters().size());

        var votersWithContributions = voters.voters().stream().filter(foo -> foo.contributions.size() > 1).toList();
        log.info("votersWithContributions size: {}", votersWithContributions.size());


        var caVotersFile = new File("/Users/giovanni/Development/workspace/catalyst-core/addresses/f13/inspect.validated_voters_ca.json");

        var zeroXVotersFile = new File("/Users/giovanni/Development/workspace/catalyst-core/addresses/f13/inspect.validated_voters_0x.json");

        var caVoters = Arrays.stream(OBJECT_MAPPER.readValue(caVotersFile, String[].class)).toList();

        var zeroXVoters = Arrays.stream(OBJECT_MAPPER.readValue(zeroXVotersFile, String[].class)).toList();

        log.info("caVoters/zeroXVoters size: {}/{}", caVoters.size(), zeroXVoters.size());


        var caFilteredVoters = voters.voters().stream().filter(foo -> caVoters.contains(foo.hir().address())).toList();
        log.info("caVoters size: {}, caFilteredVoters size: {}", voters.voters().size(), caFilteredVoters.size());

        var zeroXFilteredVoters = voters.voters
                .stream()
                .filter(voter -> zeroXVoters.contains(voter.hir().votingKey().substring(2))).toList();
        log.info("caVoters size: {}, zeroXFilteredVoters size: {}", voters.voters().size(), zeroXFilteredVoters.size());
    }


    record VoterV2(String address, Long value) {

    }

    record Initial(List<VoterV2> fund) {

    }

    record Summary(List<Initial> initial) {

    }

    @Test
    public void readVoters() throws IOException {

//        https://x.com/Catalyst_onX/status/1856421378284560732
//        https://x.com/Catalyst_onX/status/1869036243041894481

        var summaryFile = new File("/Users/giovanni/Development/workspace/catalyst-core/cexplorer-140572806.summary.json");

        var summary = OBJECT_MAPPER.readValue(summaryFile, Summary.class);
        log.info("summary.initial() size: {}", summary.initial().size());
        var voters = summary.initial().getFirst().fund();

        var caVotersFile = new File("/Users/giovanni/Development/workspace/catalyst-core/addresses/f13_3/inspect.validated_voters_ca.json");

        var zeroXVotersFile = new File("/Users/giovanni/Development/workspace/catalyst-core/addresses/f13_3/inspect.validated_voters_0x.json");

        var caVoters = Arrays.stream(OBJECT_MAPPER.readValue(caVotersFile, String[].class)).toList();

        var zeroXVoters = Arrays.stream(OBJECT_MAPPER.readValue(zeroXVotersFile, String[].class)).toList();

        log.info("caVoters/zeroXVoters size: {}/{}", caVoters.size(), zeroXVoters.size());

        var caFilteredVoters = voters.stream().filter(voter -> caVoters.contains(voter.address())).toList();
        log.info("caVoters size: {}, caFilteredVoters size: {}", voters.size(), caFilteredVoters.size());


        var fos = new FileOutputStream("/tmp/results.csv");
        fos.write("voter address, voting power, voted\n".getBytes());

        voters.forEach(voterV2 -> {
            try {
                var voted = caVoters.contains(voterV2.address());
                fos.write(String.format("%s, %d, %s\n", voterV2.address(), voterV2.value(), voted).getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        fos.close();


    }


}







//> URL="$(curl -s https://archiver.projectcatalyst.io/api/v1/archives/f19065e9-1b41-4f19-a770-1577c4823507/download | jq -r .url)"
//        > curl -o archive.tar.zstd "$URL"