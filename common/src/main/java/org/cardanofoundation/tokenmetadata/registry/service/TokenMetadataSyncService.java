package org.cardanofoundation.tokenmetadata.registry.service;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.entity.OffChainSyncState;
import org.cardanofoundation.tokenmetadata.registry.model.ChangedMappings;
import org.cardanofoundation.tokenmetadata.registry.model.Mapping;
import org.cardanofoundation.tokenmetadata.registry.model.MappingUpdateDetails;
import org.cardanofoundation.tokenmetadata.registry.model.enums.SyncStatusEnum;
import org.cardanofoundation.tokenmetadata.registry.repository.SyncStateRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
@RequiredArgsConstructor
public class TokenMetadataSyncService {

    private final GitService gitService;

    private final TokenMetadataService tokenMetadataService;

    private final TokenMappingService tokenMappingService;

    private final SyncStateRepository syncStateRepository;

    @Value("${token.metadata.job.enabled}")
    boolean isMetadataJobEnabled;

    @Getter
    private SyncStatus syncStatus;

    @PostConstruct
    void initSyncStatus() {
        if (isMetadataJobEnabled) {
            syncStatus = new SyncStatus(false, SyncStatusEnum.SYNC_NOT_STARTED);
        } else {
            syncStatus = new SyncStatus(true, SyncStatusEnum.SYNC_IN_EXTRA_JOB);
        }
    }

    public void synchronizeDatabase() {

        syncStatus.setStatus(SyncStatusEnum.SYNC_IN_PROGRESS);

        Optional<OffChainSyncState> lastSyncState = syncStateRepository.findTopByOrderByIdDesc();
        String lastHash = lastSyncState
                .map(OffChainSyncState::getLastCommitHash).orElse(null);

        long syncStart = System.currentTimeMillis();
        log.info("Starting offchain sync. Last known commit: {}", lastHash != null ? lastHash : "(none — full sync)");

        long cloneStart = System.currentTimeMillis();
        Optional<Path> repoPathOpt = gitService.cloneCardanoTokenRegistryGitRepository();

        if (repoPathOpt.isPresent()) {

            log.info("Repository ready in {} ms", System.currentTimeMillis() - cloneStart);

            Optional<String> newHashOpt = gitService.getHeadCommitHash();
            if (newHashOpt.isEmpty()) {
                log.warn("Could not determine HEAD commit hash after cloning. Falling back to full sync without hash tracking.");
            }

            if (newHashOpt.isPresent() && newHashOpt.get().equals(lastHash)) {
                log.info("No new commits since last sync. Skipping processing.");
                syncStatus.setStatus(SyncStatusEnum.SYNC_DONE);
                syncStatus.setInitialSyncDone(true);
                return;
            }

            PendingChanges pendingChanges = resolvePendingChanges(lastHash, newHashOpt, repoPathOpt.get());
            List<File> filesToProcess = pendingChanges.filesToProcess();
            log.info("Resolved {} file(s) to process, {} subject(s) to delete",
                    filesToProcess.size(), pendingChanges.subjectsToDelete().size());

            // Batch-resolve git metadata for all files in a single history walk
            Set<String> fileNames = filesToProcess.stream()
                    .map(File::getName)
                    .collect(java.util.stream.Collectors.toCollection(HashSet::new));
            long gitHistoryStart = System.currentTimeMillis();
            log.info("Resolving git history for {} file(s) in batch...", fileNames.size());
            Map<String, MappingUpdateDetails> mappingDetailsMap = gitService.getAllMappingDetails(fileNames);
            log.info("Git history resolved in {} ms", System.currentTimeMillis() - gitHistoryStart);

            long processStart = System.currentTimeMillis();
            boolean hasFailures = processMappingFiles(filesToProcess, mappingDetailsMap);
            hasFailures |= processDeletions(pendingChanges.subjectsToDelete());

            if (hasFailures) {
                log.warn("Some mappings failed to process. Commit hash will not be advanced so failed mappings are retried on next sync.");
            } else if (newHashOpt.isPresent()) {
                OffChainSyncState offChainSyncStateToSave = lastSyncState.orElse(new OffChainSyncState());
                offChainSyncStateToSave.setLastCommitHash(newHashOpt.get());
                syncStateRepository.save(offChainSyncStateToSave);
                log.info("Commit hash advanced to {}", newHashOpt.get());
            }

            log.info("Mapping processing took {} ms", System.currentTimeMillis() - processStart);

            syncStatus.setStatus(SyncStatusEnum.SYNC_DONE);
            syncStatus.setInitialSyncDone(true);
            log.info("Offchain sync complete in {} ms", System.currentTimeMillis() - syncStart);

        } else {
            log.warn("cardano-token-registry could not be cloned");
            syncStatus.setStatus(SyncStatusEnum.SYNC_ERROR);
        }

    }

    private boolean processMappingFiles(List<File> filesToProcess, Map<String, MappingUpdateDetails> mappingDetailsMap) {
        AtomicBoolean failures = new AtomicBoolean(false);
        int total = filesToProcess.size();
        int processed = 0;
        int inserted = 0;
        int skipped = 0;
        int skippedFilenameMismatch = 0;

        for (File mappingFile : filesToProcess) {
            processed++;
            Optional<Mapping> mapping = tokenMappingService.parseMappings(mappingFile);
            if (mapping.isEmpty()) {
                skipped++;
                continue;
            }

            // Filename-vs-inner-subject filter: in the upstream cardano-token-registry
            // and the testnet metadata-registry-testnet, the canonical file for a
            // token is named after its subject. A non-trivial fraction of files
            // (~90% on the testnet registry) have a filename that does not match
            // the inner `subject` field — typos, spam, or duplicates that share an
            // inner subject with a legitimate entry. Indexing them all means the
            // same DB row is upserted in File.listFiles() iteration order, which
            // is filesystem-dependent. The "winner" for such a token then varies
            // run-to-run and across deployments.
            //
            // Concrete example caught in QA: subject baa836fef0... had three files
            // (baa.../caa.../daa...). yaci-store and CF picked different ones,
            // returning different name/desc/url for the SAME on-chain token.
            //
            // Skipping mismatches gives a deterministic outcome: at most one file
            // per subject (filenames are unique), and the one we keep is the one
            // whose filename equals the registered subject — i.e. the canonical
            // entry per the registry's naming convention.
            String filenameSubject = stripJsonExtension(mappingFile.getName());
            if (!filenameSubject.equals(mapping.get().subject())) {
                log.warn("Skipping '{}': filename does not match inner subject '{}'",
                        mappingFile.getName(), mapping.get().subject());
                skippedFilenameMismatch++;
                continue;
            }

            MappingUpdateDetails updateDetails = mappingDetailsMap.get(mappingFile.getName());
            if (updateDetails == null) {
                skipped++;
                continue;
            }

            try {
                boolean metadataInserted = tokenMetadataService.insertMapping(
                        mapping.get(),
                        updateDetails.updatedAt(),
                        updateDetails.updatedBy());
                if (metadataInserted) {
                    tokenMetadataService.insertLogo(mapping.get());
                    inserted++;
                }
            } catch (Exception e) {
                failures.set(true);
                log.warn("Failed to process token '{}': {}. Continuing with next token.",
                        mapping.get().subject(), e.getMessage());
            }

            if (processed % 500 == 0) {
                log.info("Processing mappings: {}/{} done ({} inserted, {} skipped, {} filename-mismatch)",
                        processed, total, inserted, skipped, skippedFilenameMismatch);
            }
        }

        log.info("Mapping processing complete: {}/{} processed, {} inserted, {} skipped, {} filename-mismatch, failures={}",
                processed, total, inserted, skipped, skippedFilenameMismatch, failures.get());
        return failures.get();
    }

    private static String stripJsonExtension(String fileName) {
        return fileName.endsWith(".json")
                ? fileName.substring(0, fileName.length() - ".json".length())
                : fileName;
    }

    private PendingChanges resolvePendingChanges(String lastHash, Optional<String> newHashOpt, Path repoPath) {
        if (lastHash != null && newHashOpt.isPresent()) {
            log.info("Incremental sync from {} to {}", lastHash, newHashOpt.get());
            ChangedMappings changedMappings = gitService.getChangedMappings(lastHash, newHashOpt.get());
            List<File> files = changedMappings.upsertedFiles().stream()
                    .map(Path::toFile).toList();
            List<String> subjectsToDelete = changedMappings.deletedFileNames().stream()
                    .map(TokenMetadataSyncService::stripJsonExtension)
                    .toList();
            log.info("Incremental sync: processing {} changed file(s), {} deleted file(s)",
                    files.size(), subjectsToDelete.size());
            return new PendingChanges(files, subjectsToDelete);
        }

        log.info("Full sync: processing all files");
        File mappings = repoPath.toFile();
        List<File> files = Optional.ofNullable(mappings.listFiles())
                .map(Arrays::asList).orElse(List.of());
        return new PendingChanges(files, resolveStaleSubjects(files));
    }

    /**
     * Full-sync reconciliation: subjects present in the local DB whose mapping file no longer
     * exists in the registry were removed upstream (possibly while commit-hash tracking was
     * unavailable) and must be deleted locally.
     */
    private List<String> resolveStaleSubjects(List<File> presentFiles) {
        if (presentFiles.isEmpty()) {
            log.warn("Full sync found no mapping files. Skipping stale-subject cleanup as a safety measure.");
            return List.of();
        }
        Set<String> presentSubjects = new HashSet<>();
        for (File presentFile : presentFiles) {
            presentSubjects.add(stripJsonExtension(presentFile.getName()));
        }
        List<String> staleSubjects = tokenMetadataService.findAllSubjects().stream()
                .filter(subject -> !presentSubjects.contains(subject))
                .toList();
        if (!staleSubjects.isEmpty()) {
            log.info("Full sync: {} stale subject(s) no longer present in the registry will be deleted",
                    staleSubjects.size());
        }
        return staleSubjects;
    }

    private boolean processDeletions(List<String> subjectsToDelete) {
        if (subjectsToDelete.isEmpty()) {
            return false;
        }
        boolean failures = false;
        int deleted = 0;
        for (String subject : subjectsToDelete) {
            if (tokenMetadataService.deleteMapping(subject)) {
                deleted++;
                log.info("Deleted metadata for subject '{}' removed from the registry", subject);
            } else {
                failures = true;
            }
        }
        log.info("Deletion processing complete: {}/{} deleted, failures={}",
                deleted, subjectsToDelete.size(), failures);
        return failures;
    }

    /**
     * The database changes one sync run still has to apply: mapping files to upsert and
     * subjects to delete. How each side is resolved depends on the sync mode:
     *
     * <p><b>Incremental sync</b> (a last processed commit hash is stored and HEAD is known):
     * both sides come from the git tree diff between the two commits. Added/modified mapping
     * files become {@code filesToProcess}; deleted mapping files become {@code subjectsToDelete}
     * (filename minus the {@code .json} extension — for canonical registry entries the filename
     * equals the subject; for the mismatched spam files that were never indexed the resulting
     * delete is a harmless no-op).
     *
     * <p><b>Full sync</b> (first run, or commit-hash tracking unavailable): there is no diff to
     * consult, so {@code filesToProcess} is every file in the mappings folder and
     * {@code subjectsToDelete} is derived by reconciliation — DB subjects with no corresponding
     * mapping file were removed upstream while tracking was lost and must go. An empty mappings
     * folder is treated as a broken clone rather than "everything was deleted", and yields no
     * deletions.
     *
     * <p>Failed deletions, like failed upserts, prevent the commit hash from advancing so the
     * work is retried on the next run.
     *
     * @param filesToProcess   mapping files to parse and upsert into the metadata/logo tables
     * @param subjectsToDelete subjects whose metadata (and logo) rows must be removed locally
     */
    private record PendingChanges(List<File> filesToProcess, List<String> subjectsToDelete) {
    }

}
