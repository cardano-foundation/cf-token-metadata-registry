package org.cardanofoundation.tokenmetadata.registry.job.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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

}