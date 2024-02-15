package org.cardanofoundation.tokenmetadata.registry.service;

import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.model.MappingUpdateDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

@Service
@Slf4j
public class GitService {

    @Value("${git.organization:cardano-foundation}")
    private String organization;
    @Value("${git.projectName:cardano-token-registry}")
    private String projectName;
    @Value("${git.mappingsFolder:mappings}")
    private String mappingsFolderName;
    @Value("${git.tmp.folder:/tmp}")
    private String gitTempFolder;
    @Value("${git.forceClone:false}")
    private boolean forceClone;

    public Optional<Path> cloneCardanoTokenRegistryGitRepository() {
        var gitFolder = getGitFolder();

        boolean repoReady;
        if (gitFolder.exists() && (forceClone || !isGitRepo())) {
            log.info("exists and either force clone or not a git repo");
            FileSystemUtils.deleteRecursively(gitFolder);
            repoReady = cloneRepo();
        } else if (gitFolder.exists() && isGitRepo()) {
            log.info("exists and is git repo");
            repoReady = pullRebaseRepo();
        } else {
            repoReady = cloneRepo();
        }

        if (repoReady) {
            return Optional.of(getMappingsFolder());
        } else {
            return Optional.empty();
        }
    }

    private boolean cloneRepo() {
        try {
            var process = new ProcessBuilder()
                    .directory(getGitFolder().getParentFile())
                    .command("sh", "-c", String.format("git clone https://github.com/%s/%s.git", organization, projectName))
                    .start();
            var exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            log.warn(String.format("It was not possible to clone the %s project", projectName), e);
            return false;
        }
    }

    private boolean pullRebaseRepo() {
        try {
            var process = new ProcessBuilder()
                    .directory(getGitFolder())
                    .command("sh", "-c", "git pull --rebase")
                    .start();

            var exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            log.warn("it was not possible to update repo. cloning from scratch", e);
            return false;
        }
    }

    private boolean isGitRepo() {
        return getGitFolder().toPath().resolve(".git").toFile().exists();
    }

    private File getGitFolder() {
        return new File(String.format("%s/%s", gitTempFolder, projectName));
    }

    private Path getMappingsFolder() {
        return getGitFolder().toPath().resolve(mappingsFolderName);
    }

    public Optional<MappingUpdateDetails> getMappingDetails(File mappingFile) {
        try {
            var process = new ProcessBuilder()
                    .directory(getMappingsFolder().toFile())
                    .command("sh", "-c", String.format("git log -n 1 --date-order --no-merges --pretty=format:%%aE#-#%%aI %s", mappingFile.getName()))
                    .start();

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String output = bufferedReader.readLine();
            var parts = output.split("#-#");

            return Optional.of(new MappingUpdateDetails(parts[0], LocalDateTime.parse(parts[1], ISO_OFFSET_DATE_TIME)));

        } catch (IOException e) {
            log.warn(String.format("it was not possible to determine updatedBy and updatedAt for mapping file: %s", mappingFile.getName()), e);
            return Optional.empty();
        }

    }


}
