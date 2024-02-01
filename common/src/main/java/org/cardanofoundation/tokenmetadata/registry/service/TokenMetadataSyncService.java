package org.cardanofoundation.tokenmetadata.registry.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.model.Mapping;
import org.cardanofoundation.tokenmetadata.registry.model.MappingDetails;
import org.cardanofoundation.tokenmetadata.registry.model.MappingUpdateDetails;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@Slf4j
@AllArgsConstructor
public class TokenMetadataSyncService {

    private final GitService gitService;

    private final TokenMetadataService tokenMetadataService;

    private final TokenMappingService tokenMappingService;

    public void synchronizeDatabase() {

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

        } else {
            log.warn("cardano-token-registry could not be cloned");
        }

    }

}
