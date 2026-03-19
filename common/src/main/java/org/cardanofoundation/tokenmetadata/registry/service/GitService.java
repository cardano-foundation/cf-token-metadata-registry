package org.cardanofoundation.tokenmetadata.registry.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.model.MappingUpdateDetails;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.BranchConfig;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class GitService {

    @Value("${git.organization:cardano-foundation}")
    String organization;
    @Value("${git.projectName:cardano-token-registry}")
    String projectName;
    @Value("${git.mappingsFolder:mappings}")
    String mappingsFolderName;
    @Value("${git.tmp.folder:/tmp}")
    String gitTempFolder;
    @Value("${git.forceClone:false}")
    boolean forceClone;

    Git git;

    @PostConstruct
    void validateConfig() {
        if (gitTempFolder == null || gitTempFolder.isBlank()) {
            log.warn("git.tmp.folder is blank, defaulting to system temp directory");
            gitTempFolder = System.getProperty("java.io.tmpdir");
        }
    }

    @PreDestroy
    void cleanup() {
        if (git != null) {
            git.close();
            git = null;
        }
    }

    public Optional<Path> cloneCardanoTokenRegistryGitRepository() {
        var gitFolder = getGitFolder();

        boolean repoReady;
        if (gitFolder.exists() && (forceClone || !isGitRepo())) {
            log.info("exists and either force clone or not a git repo");
            cleanup();
            FileSystemUtils.deleteRecursively(gitFolder);
            repoReady = cloneRepo();
        } else if (gitFolder.exists() && isGitRepo()) {
            log.info("exists and is git repo");
            repoReady = openExistingRepo() && pullRebaseRepo();
        } else {
            repoReady = cloneRepo();
        }

        if (repoReady) {
            return Optional.of(getMappingsFolder());
        } else {
            return Optional.empty();
        }
    }

    private boolean openExistingRepo() {
        try {
            cleanup();
            git = Git.open(getGitFolder());
            return true;
        } catch (IOException e) {
            log.warn("Failed to open existing git repository", e);
            return false;
        }
    }

    private boolean cloneRepo() {
        try {
            var url = String.format("https://github.com/%s/%s.git", organization, projectName);
            git = Git.cloneRepository()
                    .setURI(url)
                    .setDirectory(getGitFolder())
                    .call();
            return true;
        } catch (GitAPIException e) {
            log.warn("It was not possible to clone the {} project", projectName, e);
            return false;
        }
    }

    private boolean pullRebaseRepo() {
        try {
            var remotes = git.remoteList().call();
            if (remotes.isEmpty()) {
                log.info("No remote configured, skipping pull");
                return true;
            }
            git.pull()
                    .setRebase(BranchConfig.BranchRebaseMode.REBASE)
                    .call();
            return true;
        } catch (GitAPIException e) {
            log.warn("it was not possible to update repo. cloning from scratch", e);
            return false;
        }
    }

private boolean isGitRepo() {
        return getGitFolder().toPath().resolve(".git").toFile().exists();
    }

    private File getGitFolder() {
        return Path.of(gitTempFolder).resolve(projectName).toFile();
    }

    private Path getMappingsFolder() {
        return getGitFolder().toPath().resolve(mappingsFolderName);
    }

    public Optional<MappingUpdateDetails> getMappingDetails(File mappingFile) {
        if (git == null) {
            log.warn("Git repository not initialized");
            return Optional.empty();
        }
        try {
            String relativePath = mappingsFolderName + "/" + mappingFile.getName();

            Iterable<RevCommit> commits = git.log()
                    .addPath(relativePath)
                    .setRevFilter(RevFilter.NO_MERGES)
                    .setMaxCount(1)
                    .call();

            for (RevCommit commit : commits) {
                PersonIdent author = commit.getAuthorIdent();
                String email = author.getEmailAddress();
                LocalDateTime updatedAt = LocalDateTime.ofInstant(
                        author.getWhenAsInstant(), ZoneOffset.UTC);
                return Optional.of(new MappingUpdateDetails(email, updatedAt));
            }

            return Optional.empty();
        } catch (GitAPIException e) {
            log.warn("it was not possible to determine updatedBy and updatedAt for mapping file: {}",
                    mappingFile.getName(), e);
            return Optional.empty();
        }
    }

    public Optional<String> getHeadCommitHash() {
        if (git == null) {
            log.warn("Git repository not initialized");
            return Optional.empty();
        }
        try {
            Repository repository = git.getRepository();
            ObjectId head = repository.resolve("HEAD");
            if (head != null) {
                return Optional.of(head.name());
            }
        } catch (IOException e) {
            log.warn("Failed to get HEAD commit hash", e);
        }
        return Optional.empty();
    }

    public List<Path> getChangedFiles(String fromHash, String toHash) {
        if (git == null) {
            log.warn("Git repository not initialized");
            return List.of();
        }
        try {
            Repository repository = git.getRepository();

            ObjectId oldId = repository.resolve(fromHash);
            ObjectId newId = repository.resolve(toHash);

            if (oldId == null || newId == null) {
                log.warn("Could not resolve commit hashes: {} -> {}", fromHash, toHash);
                return List.of();
            }

            AbstractTreeIterator oldTree = prepareTreeParser(repository, oldId);
            AbstractTreeIterator newTree = prepareTreeParser(repository, newId);

            List<DiffEntry> diffs = git.diff()
                    .setOldTree(oldTree)
                    .setNewTree(newTree)
                    .setPathFilter(PathFilter.create(mappingsFolderName))
                    .call();

            Path repoRoot = getGitFolder().toPath();
            return diffs.stream()
                    .filter(d -> d.getChangeType() == DiffEntry.ChangeType.ADD
                            || d.getChangeType() == DiffEntry.ChangeType.MODIFY)
                    .map(DiffEntry::getNewPath)
                    .filter(path -> path.endsWith(".json"))
                    .map(repoRoot::resolve)
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to get changed files between {} and {}", fromHash, toHash, e);
        }
        return List.of();
    }

    private AbstractTreeIterator prepareTreeParser(Repository repository, ObjectId objectId) throws IOException {
        try (RevWalk walk = new RevWalk(repository)) {
            var commit = walk.parseCommit(objectId);
            var treeParser = new CanonicalTreeParser();
            try (var reader = repository.newObjectReader()) {
                treeParser.reset(reader, commit.getTree().getId());
            }
            return treeParser;
        }
    }

}
