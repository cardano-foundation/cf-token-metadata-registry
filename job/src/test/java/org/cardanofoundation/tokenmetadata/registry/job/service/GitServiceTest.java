package org.cardanofoundation.tokenmetadata.registry.job.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

@SpringBootTest(classes = GitService.class)
@Slf4j
class GitServiceTest {

    @Autowired
    private GitService gitService;

    @Test
    public void cloneProject() {

        gitService.cloneCardanoTokenRegistryGitRepository()
                .ifPresent(mappingsPath -> {

                    var mappingsDirectory = mappingsPath.toFile();
                    Arrays.stream(mappingsDirectory.listFiles()).forEach(file -> log.info("File: {}", file.getName()));

                });

    }

    @Test
    public void getMappingUpdateDetails() {


        var foo = LocalDateTime.now();

        log.info("foo: {}", foo.toString());

//        LocalDateTime.parse(, DateTimeFormatter.)

//        2024-01-26T14:45:22.371828
//        2021-04-15T18:46:52+02:00
        var detailsOpt = gitService.getMappingDetails(new File("/tmp/cardano-token-registry/mappings/00000002df633853f6a47465c9496721d2d5b1291b8398016c0e87ae6e7574636f696e.json"));
    }

}