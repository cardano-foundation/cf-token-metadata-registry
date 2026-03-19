package org.cardanofoundation.tokenmetadata.registry.service;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GitServiceTest {

    private GitService gitService;

    @TempDir
    Path tempDir;

    private Git testRepo;

    @BeforeEach
    void setUp() {
        gitService = new GitService();
        gitService.organization = "test-org";
        gitService.projectName = "test-repo";
        gitService.mappingsFolderName = "mappings";
        gitService.gitTempFolder = tempDir.toString();
        gitService.forceClone = false;
    }

    @AfterEach
    void tearDown() {
        if (testRepo != null) {
            testRepo.close();
            testRepo = null;
        }
        gitService.cleanup();
    }

    private Git initRepoWithMappings() throws GitAPIException, IOException {
        Path repoDir = tempDir.resolve("test-repo");
        Files.createDirectories(repoDir.resolve("mappings"));
        Git git = Git.init().setDirectory(repoDir.toFile()).call();

        Files.writeString(repoDir.resolve("README.md"), "init");
        git.add().addFilepattern("README.md").call();
        git.commit().setMessage("initial commit")
                .setAuthor(new PersonIdent("Init", "init@test.com"))
                .call();

        return git;
    }

    private RevCommit addMappingFile(Git git, String fileName, String content, String email) throws Exception {
        Path mappingsDir = git.getRepository().getWorkTree().toPath().resolve("mappings");
        Files.createDirectories(mappingsDir);
        Files.writeString(mappingsDir.resolve(fileName), content);
        git.add().addFilepattern("mappings/" + fileName).call();
        return git.commit().setMessage("add " + fileName)
                .setAuthor(new PersonIdent("Test Author", email))
                .call();
    }

    @Nested
    class GetHeadCommitHash {

        @Test
        void returnsHashFromRepo() throws Exception {
            testRepo = initRepoWithMappings();
            gitService.git = testRepo;

            var result = gitService.getHeadCommitHash();

            assertThat(result).isPresent();
            assertThat(result.get()).hasSize(40).matches("[0-9a-f]+");
        }

        @Test
        void returnsEmptyWhenGitIsNull() {
            var result = gitService.getHeadCommitHash();

            assertThat(result).isEmpty();
        }

        @Test
        void matchesActualHeadCommit() throws Exception {
            testRepo = initRepoWithMappings();
            gitService.git = testRepo;
            String expectedHash = testRepo.getRepository().resolve("HEAD").name();

            var result = gitService.getHeadCommitHash();

            assertThat(result).hasValue(expectedHash);
        }

        @Test
        void updatesAfterNewCommit() throws Exception {
            testRepo = initRepoWithMappings();
            gitService.git = testRepo;
            String firstHash = gitService.getHeadCommitHash().orElseThrow();

            addMappingFile(testRepo, "token1.json", "{}", "dev@test.com");
            String secondHash = gitService.getHeadCommitHash().orElseThrow();

            assertThat(secondHash).isNotEqualTo(firstHash);
        }
    }

    @Nested
    class GetMappingDetails {

        @Test
        void returnsAuthorEmailAndDate() throws Exception {
            testRepo = initRepoWithMappings();
            gitService.git = testRepo;
            String email = "author@cardano.org";
            RevCommit commit = addMappingFile(testRepo, "token1.json", "{\"subject\":\"abc\"}", email);

            var result = gitService.getMappingDetails(new File("token1.json"));

            assertThat(result).isPresent();
            assertThat(result.get().updatedBy()).isEqualTo(email);
            LocalDateTime expectedTime = LocalDateTime.ofInstant(
                    commit.getAuthorIdent().getWhenAsInstant(), ZoneOffset.UTC);
            assertThat(result.get().updatedAt()).isEqualTo(expectedTime);
        }

        @Test
        void returnsEmptyForUnknownFile() throws Exception {
            testRepo = initRepoWithMappings();
            gitService.git = testRepo;

            var result = gitService.getMappingDetails(new File("nonexistent.json"));

            assertThat(result).isEmpty();
        }

        @Test
        void returnsLatestNonMergeCommit() throws Exception {
            testRepo = initRepoWithMappings();
            gitService.git = testRepo;

            addMappingFile(testRepo, "token1.json", "{\"v\":1}", "first@test.com");
            addMappingFile(testRepo, "token1.json", "{\"v\":2}", "latest@test.com");

            var result = gitService.getMappingDetails(new File("token1.json"));

            assertThat(result).isPresent();
            assertThat(result.get().updatedBy()).isEqualTo("latest@test.com");
        }

        @Test
        void returnsEmptyWhenGitIsNull() {
            var result = gitService.getMappingDetails(new File("token1.json"));

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class GetChangedFiles {

        @Test
        void returnsAddedJsonFilesInMappingsFolder() throws Exception {
            testRepo = initRepoWithMappings();
            gitService.git = testRepo;
            String fromHash = testRepo.getRepository().resolve("HEAD").name();

            addMappingFile(testRepo, "token1.json", "{}", "dev@test.com");
            String toHash = testRepo.getRepository().resolve("HEAD").name();

            List<Path> changed = gitService.getChangedFiles(fromHash, toHash);

            assertThat(changed).hasSize(1);
            assertThat(changed.get(0).getFileName().toString()).isEqualTo("token1.json");
        }

        @Test
        void returnsModifiedFiles() throws Exception {
            testRepo = initRepoWithMappings();
            gitService.git = testRepo;
            addMappingFile(testRepo, "token1.json", "{\"v\":1}", "dev@test.com");
            String fromHash = testRepo.getRepository().resolve("HEAD").name();

            addMappingFile(testRepo, "token1.json", "{\"v\":2}", "dev@test.com");
            String toHash = testRepo.getRepository().resolve("HEAD").name();

            List<Path> changed = gitService.getChangedFiles(fromHash, toHash);

            assertThat(changed).hasSize(1);
            assertThat(changed.get(0).getFileName().toString()).isEqualTo("token1.json");
        }

        @Test
        void filtersOutNonJsonFiles() throws Exception {
            testRepo = initRepoWithMappings();
            gitService.git = testRepo;
            String fromHash = testRepo.getRepository().resolve("HEAD").name();

            addMappingFile(testRepo, "readme.txt", "text", "dev@test.com");
            addMappingFile(testRepo, "token1.json", "{}", "dev@test.com");
            String toHash = testRepo.getRepository().resolve("HEAD").name();

            List<Path> changed = gitService.getChangedFiles(fromHash, toHash);

            assertThat(changed).hasSize(1);
            assertThat(changed.get(0).getFileName().toString()).isEqualTo("token1.json");
        }

        @Test
        void filtersOutFilesOutsideMappingsFolder() throws Exception {
            testRepo = initRepoWithMappings();
            gitService.git = testRepo;
            String fromHash = testRepo.getRepository().resolve("HEAD").name();

            Path repoDir = testRepo.getRepository().getWorkTree().toPath();
            Files.writeString(repoDir.resolve("other.json"), "{}");
            testRepo.add().addFilepattern("other.json").call();
            testRepo.commit().setMessage("add other.json")
                    .setAuthor(new PersonIdent("Dev", "dev@test.com"))
                    .call();

            addMappingFile(testRepo, "token1.json", "{}", "dev@test.com");
            String toHash = testRepo.getRepository().resolve("HEAD").name();

            List<Path> changed = gitService.getChangedFiles(fromHash, toHash);

            assertThat(changed).hasSize(1);
            assertThat(changed.get(0).getFileName().toString()).isEqualTo("token1.json");
        }

        @Test
        void filtersOutDeletedFiles() throws Exception {
            testRepo = initRepoWithMappings();
            gitService.git = testRepo;
            addMappingFile(testRepo, "token1.json", "{}", "dev@test.com");
            addMappingFile(testRepo, "token2.json", "{}", "dev@test.com");
            String fromHash = testRepo.getRepository().resolve("HEAD").name();

            Path repoDir = testRepo.getRepository().getWorkTree().toPath();
            Files.delete(repoDir.resolve("mappings/token1.json"));
            testRepo.rm().addFilepattern("mappings/token1.json").call();
            testRepo.commit().setMessage("delete token1")
                    .setAuthor(new PersonIdent("Dev", "dev@test.com"))
                    .call();
            String toHash = testRepo.getRepository().resolve("HEAD").name();

            List<Path> changed = gitService.getChangedFiles(fromHash, toHash);

            assertThat(changed).isEmpty();
        }

        @Test
        void returnsEmptyForSameHash() throws Exception {
            testRepo = initRepoWithMappings();
            gitService.git = testRepo;
            String hash = testRepo.getRepository().resolve("HEAD").name();

            List<Path> changed = gitService.getChangedFiles(hash, hash);

            assertThat(changed).isEmpty();
        }

        @Test
        void returnsEmptyForInvalidHashes() throws Exception {
            testRepo = initRepoWithMappings();
            gitService.git = testRepo;

            List<Path> changed = gitService.getChangedFiles(
                    "0000000000000000000000000000000000000000",
                    "1111111111111111111111111111111111111111");

            assertThat(changed).isEmpty();
        }

        @Test
        void returnsMultipleChangedFiles() throws Exception {
            testRepo = initRepoWithMappings();
            gitService.git = testRepo;
            String fromHash = testRepo.getRepository().resolve("HEAD").name();

            addMappingFile(testRepo, "token1.json", "{}", "dev@test.com");
            addMappingFile(testRepo, "token2.json", "{}", "dev@test.com");
            addMappingFile(testRepo, "token3.json", "{}", "dev@test.com");
            String toHash = testRepo.getRepository().resolve("HEAD").name();

            List<Path> changed = gitService.getChangedFiles(fromHash, toHash);

            assertThat(changed).hasSize(3)
                    .extracting(p -> p.getFileName().toString())
                    .containsExactlyInAnyOrder("token1.json", "token2.json", "token3.json");
        }
    }

    @Nested
    class ValidateConfig {

        @Test
        void defaultsToSystemTempWhenBlank() {
            gitService.gitTempFolder = "";

            gitService.validateConfig();

            assertThat(gitService.gitTempFolder).isEqualTo(System.getProperty("java.io.tmpdir"));
        }

        @Test
        void keepsValueWhenNotBlank() {
            String customPath = "/custom/path";
            gitService.gitTempFolder = customPath;

            gitService.validateConfig();

            assertThat(gitService.gitTempFolder).isEqualTo(customPath);
        }
    }

    @Nested
    class CleanupTest {

        @Test
        void closesAndNullsGit() throws Exception {
            testRepo = initRepoWithMappings();
            gitService.git = testRepo;

            gitService.cleanup();

            assertThat(gitService.git).isNull();
            // prevent double-close in tearDown
            testRepo = null;
        }

        @Test
        void handlesNullGitGracefully() {
            gitService.cleanup();
        }
    }

    @Nested
    class CloneCardanoTokenRegistryGitRepository {

        @Test
        void pullsWhenRepoAlreadyExists() throws Exception {
            Path remoteDir = tempDir.resolve("remote-repo");
            try (Git remoteGit = Git.init().setBare(true).setDirectory(remoteDir.toFile()).call()) {
                Path seedDir = tempDir.resolve("seed");
                try (Git seedGit = Git.init().setDirectory(seedDir.toFile()).call()) {
                    Files.createDirectories(seedDir.resolve("mappings"));
                    Files.writeString(seedDir.resolve("mappings/token.json"), "{}");
                    seedGit.add().addFilepattern(".").call();
                    seedGit.commit().setMessage("init")
                            .setAuthor(new PersonIdent("Seed", "seed@test.com"))
                            .call();
                    seedGit.push().setRemote(remoteDir.toUri().toString()).call();
                }

                Path repoDir = tempDir.resolve("test-repo");
                try (Git clonedGit = Git.cloneRepository()
                        .setURI(remoteDir.toUri().toString())
                        .setDirectory(repoDir.toFile())
                        .call()) {
                }
            }

            var result = gitService.cloneCardanoTokenRegistryGitRepository();

            assertThat(result).isPresent();
            assertThat(result.get().toFile()).exists();
            assertThat(result.get().getFileName().toString()).isEqualTo("mappings");
        }

        @Test
        void returnsEmptyWhenCloneFails() {
            var result = gitService.cloneCardanoTokenRegistryGitRepository();

            assertThat(result).isEmpty();
        }

        @Test
        void forceCloneDeletesExistingNonGitDir() throws Exception {
            gitService.forceClone = true;

            Path repoDir = tempDir.resolve("test-repo");
            Files.createDirectories(repoDir.resolve("mappings"));
            Files.writeString(repoDir.resolve("some-file.txt"), "stale");

            var result = gitService.cloneCardanoTokenRegistryGitRepository();

            assertThat(result).isEmpty();
            assertThat(repoDir.resolve("some-file.txt")).doesNotExist();
        }
    }
}
