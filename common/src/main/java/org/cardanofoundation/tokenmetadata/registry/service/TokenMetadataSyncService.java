package org.cardanofoundation.tokenmetadata.registry.service;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.model.Mapping;
import org.cardanofoundation.tokenmetadata.registry.model.MappingDetails;
import org.cardanofoundation.tokenmetadata.registry.model.MappingUpdateDetails;
import org.cardanofoundation.tokenmetadata.registry.model.enums.SyncStatusEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class TokenMetadataSyncService {

    private final GitService gitService;

    private final TokenMetadataService tokenMetadataService;

    private final TokenMappingService tokenMappingService;

    @Value("${token.metadata.job.enabled}")
    private boolean isMetadataJobEnabled;

    @Getter
    private SyncStatus syncStatus;

    @PostConstruct
    private void initSyncStatus() {
        if(isMetadataJobEnabled) {
            syncStatus = new SyncStatus(false, SyncStatusEnum.SYNC_NOT_STARTED);
        } else {
            syncStatus = new SyncStatus(true, SyncStatusEnum.SYNC_IN_EXTRA_JOB);
        }
    }

    public void synchronizeDatabase() {

        syncStatus.setSyncStatus(SyncStatusEnum.SYNC_IN_PROGRESS);

        Optional<Path> repoPathOpt = gitService.cloneCardanoTokenRegistryGitRepository();

        if (repoPathOpt.isPresent()) {

            Path repoPath = repoPathOpt.get();

            File mappings = repoPath.toFile();

            Optional.ofNullable(mappings.listFiles())
                    .map(Arrays::asList)
                    .stream()
                    .flatMap(Collection::stream)
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
                        tokenMetadataService.insertMapping(mappingDetails.mapping(),
                                mappingDetails.mappingUpdateDetails().updatedAt(),
                                mappingDetails.mappingUpdateDetails().updatedBy());

                        tokenMetadataService.insertLogo(mappingDetails.mapping());

                    });

            syncStatus.setSyncStatus(SyncStatusEnum.SYNC_DONE);
            syncStatus.setInitialSyncDone(true);

        } else {
            log.warn("cardano-token-registry could not be cloned");
            syncStatus.setSyncStatus(SyncStatusEnum.SYNC_ERROR);
        }

    }


}
