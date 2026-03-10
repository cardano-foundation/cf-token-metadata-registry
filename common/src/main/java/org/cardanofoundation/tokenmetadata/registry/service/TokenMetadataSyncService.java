package org.cardanofoundation.tokenmetadata.registry.service;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.entity.OffChainSyncState;
import org.cardanofoundation.tokenmetadata.registry.model.Mapping;
import org.cardanofoundation.tokenmetadata.registry.model.MappingDetails;
import org.cardanofoundation.tokenmetadata.registry.model.MappingUpdateDetails;
import org.cardanofoundation.tokenmetadata.registry.model.enums.SyncStatusEnum;
import org.cardanofoundation.tokenmetadata.registry.repository.SyncStateRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class TokenMetadataSyncService {

    private final GitService gitService;

    private final TokenMetadataService tokenMetadataService;

    private final TokenMappingService tokenMappingService;

    private final SyncStateRepository syncStateRepository;

    @Value("${token.metadata.job.enabled}")
    private boolean isMetadataJobEnabled;

    @Getter
    private SyncStatus syncStatus;

    @PostConstruct
    private void initSyncStatus() {
        if (isMetadataJobEnabled) {
            syncStatus = new SyncStatus(false, SyncStatusEnum.SYNC_NOT_STARTED);
        } else {
            syncStatus = new SyncStatus(true, SyncStatusEnum.SYNC_IN_EXTRA_JOB);
        }
    }

    public void synchronizeDatabase() {

        syncStatus.setSyncStatus(SyncStatusEnum.SYNC_IN_PROGRESS);

        Optional<OffChainSyncState> lastSyncState = syncStateRepository.findTopByOrderByIdDesc();
        String lastHash = lastSyncState
                .map(OffChainSyncState::getLastCommitHash).orElse(null);

        Optional<Path> repoPathOpt = gitService.cloneCardanoTokenRegistryGitRepository();

        if (repoPathOpt.isPresent()) {

            Optional<String> newHashOpt = gitService.getHeadCommitHash();
            if (newHashOpt.isPresent() && newHashOpt.get().equals(lastHash)) {
                log.info("No new commits since last sync. Skipping processing.");
                syncStatus.setSyncStatus(SyncStatusEnum.SYNC_DONE);
                syncStatus.setInitialSyncDone(true);
                return;
            }

            List<File> filesToProcess;
            if (lastHash != null && newHashOpt.isPresent()) {
                log.info("Incremental sync from {} to {}", lastHash, newHashOpt.get());
                filesToProcess = gitService.getChangedFiles(lastHash, newHashOpt.get()).stream()
                        .map(Path::toFile).toList();
                log.info("Incremental sync: processing {} changed file(s)", filesToProcess.size());
            } else {
                log.info("Full sync: processing all files");
                File mappings = repoPathOpt.get().toFile();
                filesToProcess = Optional.ofNullable(mappings.listFiles())
                        .map(Arrays::asList).orElse(List.of());
            }

            filesToProcess.stream()
                    .flatMap(mappingFile -> {

                        Optional<Mapping> mapping = tokenMappingService.parseMappings(mappingFile);
                        Optional<MappingUpdateDetails> mappingUpdateDetails = gitService.getMappingDetails(mappingFile);

                        if (mapping.isPresent() && mappingUpdateDetails.isPresent()) {
                            return Stream.of(new MappingDetails(mapping.get(), mappingUpdateDetails.get()));
                        } else {
                            return Stream.empty();
                        }

                    })
                    .forEach(mappingDetails -> {
                        try {
                            boolean metadataInserted = tokenMetadataService.insertMapping(
                                    mappingDetails.mapping(),
                                    mappingDetails.mappingUpdateDetails().updatedAt(),
                                    mappingDetails.mappingUpdateDetails().updatedBy());

                            // Only insert logo if metadata was successfully inserted
                            if (metadataInserted) {
                                tokenMetadataService.insertLogo(mappingDetails.mapping());
                            }
                        } catch (Exception e) {
                            log.warn("Failed to process token '{}': {}. Continuing with next token.",
                                    mappingDetails.mapping().subject(), e.getMessage());
                        }
                    });

            if (newHashOpt.isPresent()) {
                OffChainSyncState offChainSyncStateToSave = lastSyncState.orElse(new OffChainSyncState());
                offChainSyncStateToSave.setLastCommitHash(newHashOpt.get());
                syncStateRepository.save(offChainSyncStateToSave);
            }

            syncStatus.setSyncStatus(SyncStatusEnum.SYNC_DONE);
            syncStatus.setInitialSyncDone(true);

        } else {
            log.warn("cardano-token-registry could not be cloned");
            syncStatus.setSyncStatus(SyncStatusEnum.SYNC_ERROR);
        }

    }

}
