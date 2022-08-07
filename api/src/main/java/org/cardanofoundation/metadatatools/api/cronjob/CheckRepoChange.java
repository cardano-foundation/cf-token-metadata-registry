package org.cardanofoundation.metadatatools.api.cronjob;

import antlr.Token;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.cardanofoundation.metadatatools.api.model.data.SubmitToken;
import org.cardanofoundation.metadatatools.api.model.rest.TokenMetadata;
import org.cardanofoundation.metadatatools.api.repository.SubmitTokenRepo;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Log4j2
public class CheckRepoChange {

    @Value("${git.local.repository.path}")
    private String gitLocalRepoPath;
    @Value("${git.repository.url}")
    private String gitRepoUrl;
    @Autowired
    private SubmitTokenRepo submitTokenRepo;

    @Scheduled(cron = "0 0/3 * * * *")
    private void checkRepoFunc() throws IOException, GitAPIException {
        log.info("Current repo path: " + gitLocalRepoPath);
        File localRepoDir = new File(gitLocalRepoPath);
        ObjectMapper mapper = new ObjectMapper();
        if (!localRepoDir.exists()){
            localRepoDir.mkdir();
        }
        TextProgressMonitor consoleProgressMonitor = new TextProgressMonitor(new PrintWriter(System.out));
        if(localRepoDir.listFiles().length > 0) {
            // was cloned before
            log.info("Call pull request to check status of cardano token registry repository !");
            Git git = Git.open(localRepoDir);
            Repository exsistRepo = git.getRepository();
            ObjectId oldHead = exsistRepo.resolve("HEAD^{tree}");
            PullResult pullResult = git.pull().setProgressMonitor(consoleProgressMonitor).setRemote("origin")
                    .setRemoteBranchName("master").call();
            if (pullResult.isSuccessful()) {
                log.info("Pull Successfull");
                ObjectId newHead = exsistRepo.resolve("HEAD^{tree}");
                ObjectReader reader = exsistRepo.newObjectReader();
                CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
                oldTreeIter.reset(reader, oldHead);
                CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
                newTreeIter.reset(reader, newHead);
                List<DiffEntry> diffs = git.diff()
                        .setNewTree(newTreeIter)
                        .setOldTree(oldTreeIter)
                        .call();
                if (diffs.size() > 0) {
                    List<String> listChangesFile = new ArrayList<>();
                    List<String> listDeletedFile = new ArrayList<>();
                    String mappingsPath = "mappings/(.*)\\.json";
                    Pattern pattern = Pattern.compile(mappingsPath);
                    Matcher matcher;
                    for (DiffEntry diff : diffs) {
                        if(diff.getChangeType().equals(DiffEntry.ChangeType.DELETE)) {
                            matcher = pattern.matcher( diff.getOldPath() );
                            if(matcher.matches()) {
                                listDeletedFile.add(matcher.group(1));
                            }
                        } else {
                            matcher = pattern.matcher( diff.getNewPath() );
                            if(matcher.matches()) {
                                listChangesFile.add(diff.getNewPath());
                            }
                        }
                    }
                    if (listChangesFile.size() > 0) {
                        List<SubmitToken> submitTokensChangedList = new ArrayList<>();
                        TokenMetadata tokenMetadataMapping;
                        for (String changeFile : listChangesFile) {
                            try {
                                tokenMetadataMapping = mapper.readValue(new File(localRepoDir + "/" + changeFile), TokenMetadata.class);
                                submitTokensChangedList.add(convertToSubmitToken(tokenMetadataMapping));
                            } catch (Exception ex) {
                                log.error("Parse JSON Failed!" , ex);
                                continue;
                            }
                        }
                        if (submitTokensChangedList.size() > 0) {
                            try {
                                submitTokenRepo.saveAll(submitTokensChangedList);
                            } catch (Exception ex) {
                                log.error("Save all changes failed!" , ex);
                            }
                        }
                    }
                    if (listDeletedFile.size() > 0) {
                        List<SubmitToken> submitTokensRemovedList = new ArrayList<>();
                        for (String deleteFile : listDeletedFile) {
                            try {
                                submitTokensRemovedList.add((new SubmitToken(deleteFile)));
                            } catch (Exception ex) {
                                log.error("Parse JSON Failed!" , ex);
                                continue;
                            }
                        }
                        if (submitTokensRemovedList.size() > 0) {
                            try {
                                submitTokenRepo.deleteAll(submitTokensRemovedList);
                            } catch (Exception ex) {
                                log.error("Delete all remove file failed!" , ex);
                            }
                        }
                    }
                } else {
                    log.info("Everything up to date !");
                }
            } else {
                log.error("Pull Failed!");
            }
        } else {
            // first time clone repo
            try {
                log.info("\n>>> Cloning repository\n");
                Repository repoClone = Git.cloneRepository().setProgressMonitor(consoleProgressMonitor).setDirectory(localRepoDir)
                        .setURI(gitRepoUrl).call().getRepository();
                log.info("\n>>> Cloning done !\n");
                localRepoDir = new File(gitLocalRepoPath + "/mappings");
                if(localRepoDir.listFiles().length > 0) {
                    TokenMetadata tokenMetadataMapping;
                    List<SubmitToken> submitTokensClonedList = new ArrayList<>();
                    for (File jsonFile : localRepoDir.listFiles()){
                        try {
                            tokenMetadataMapping = mapper.readValue(jsonFile, TokenMetadata.class);
                            submitTokensClonedList.add(convertToSubmitToken(tokenMetadataMapping));
                        } catch (Exception ex) {
                            log.error("Parse failed, maybe incorrect file input!" , ex);
                            continue;
                        }
                    }
                    if (submitTokensClonedList.size() > 0) {
                        try {
                            submitTokenRepo.saveAll(submitTokensClonedList);
                        } catch (Exception ex) {
                            log.error("Save all clone file failed!" , ex);
                        }
                    }
                } else {
                    log.info("Empty mappings folder from Git repositoty !");
                }
            } catch (GitAPIException ex) {
                log.error("Clone Failed!" , ex);
            }
        }
    }
    public SubmitToken convertToSubmitToken(TokenMetadata tokenMetadata) {
        SubmitToken submitToken = new SubmitToken();
        if (tokenMetadata.getSubject() != null) submitToken.setSubject(tokenMetadata.getSubject());
        if (tokenMetadata.getPolicy() != null) submitToken.setPolicy(tokenMetadata.getPolicy());
        if (tokenMetadata.getName() != null && !tokenMetadata.getName().getValue().isEmpty() && tokenMetadata.getName().getValue() != null) submitToken.setName(tokenMetadata.getName().getValue());
        if (tokenMetadata.getDescription() != null && !tokenMetadata.getDescription().getValue().isEmpty() && tokenMetadata.getDescription().getValue() != null) submitToken.setDescription(tokenMetadata.getDescription().getValue());
        if (tokenMetadata.getDecimals() != null && tokenMetadata.getDecimals().getValue() != null) submitToken.setDecimals(tokenMetadata.getDecimals().getValue());
        if (tokenMetadata.getLogo() != null && !tokenMetadata.getLogo().getValue().isEmpty() && tokenMetadata.getLogo().getValue() != null) submitToken.setLogo(tokenMetadata.getLogo().getValue());
        if (tokenMetadata.getTicker() != null && !tokenMetadata.getTicker().getValue().isEmpty() && tokenMetadata.getTicker().getValue() != null) submitToken.setTicker(tokenMetadata.getTicker().getValue());
        if (tokenMetadata.getUrl() != null && !tokenMetadata.getUrl().getValue().isEmpty() && tokenMetadata.getUrl().getValue() != null) submitToken.setUrl(tokenMetadata.getUrl().getValue());
        submitToken.setUpdated(new Date());
        submitToken.setStatus("approved");
        submitToken.setProperties(tokenMetadata);
        return submitToken;
    }
}
