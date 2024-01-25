package org.cardanofoundation.tokenmetadata.registry.job.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

@Service
public class GitService {

    @Value("${git.organization:cardano-foundation}")
    private String organization;
    @Value("${git.projectName:cardano-token-registry}")
    private String projectName;
    @Value("${git.mappingsFolder:mappings}")
    private String mappingsFolderName;
    @Value("${git.tmp.folder:/tmp}")
    private String gitTempFolder;

    public Optional<Path> cloneCardanoTokenRegistryGitRepository() {
        var gitFolder = new File(String.format("%s/%s", gitTempFolder, projectName));
        if (gitFolder.exists()) {
            FileSystemUtils.deleteRecursively(gitFolder);
        }
        try {

            var process = new ProcessBuilder()
                    .directory(gitFolder.getParentFile())
                    .command("sh", "-c", String.format("git clone https://github.com/%s/%s.git", organization, projectName))
                    .start();

            var exitCode = process.waitFor();

            if (exitCode == 0) {
                return Optional.of(gitFolder.toPath().resolve(mappingsFolderName));
            } else {
                return Optional.empty();
            }


        } catch (Exception e) {
            return Optional.empty();
        }


    }


}
