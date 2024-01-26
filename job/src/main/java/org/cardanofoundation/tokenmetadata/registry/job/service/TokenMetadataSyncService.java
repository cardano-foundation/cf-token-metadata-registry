package org.cardanofoundation.tokenmetadata.registry.job.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.job.model.Item;
import org.cardanofoundation.tokenmetadata.registry.job.model.Mapping;
import org.cardanofoundation.tokenmetadata.registry.job.model.MappingDetails;
import org.cardanofoundation.tokenmetadata.registry.job.model.MappingUpdateDetails;
import org.cardanofoundation.tokenmetadata.registry.job.persistence.TokenMetadataDao;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@Slf4j
@AllArgsConstructor
public class TokenMetadataSyncService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final GitService gitService;

    private final TokenMetadataDao tokenMetadataDao;

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

                        Optional<Mapping> mapping = parseMappings(mappingFile);
                        Optional<MappingUpdateDetails> mappingUpdateDetails = gitService.getMappingDetails(mappingFile);

                        if (mapping.isPresent() && mappingUpdateDetails.isPresent()) {
                            return Stream.of(new MappingDetails(mapping.get(), mappingUpdateDetails.get()));
                        } else {
                            return Stream.empty();
                        }

                    })
                    .forEach(mappingDetails -> {
                        Mapping mapping = mappingDetails.mapping();
                        var updatedAt = Timestamp.valueOf(mappingDetails.mappingUpdateDetails().updatedAt());
                        tokenMetadataDao.insertTokenMetadata(mapping.subject(),
                                "mainnet",
                                Optional.ofNullable(mapping.policy()),
                                Optional.ofNullable(mapping.name()).map(Item::value),
                                Optional.ofNullable(mapping.ticker()).map(Item::value),
                                Optional.ofNullable(mapping.url()).map(Item::value),
                                Optional.ofNullable(mapping.description()).map(Item::value),
                                Optional.ofNullable(mapping.decimals()).map(Item::value).map(Integer::valueOf),
                                updatedAt,
                                mappingDetails.mappingUpdateDetails().updatedBy(),
                                mapping
                        );
                    });

        } else {
            log.warn("cardano-token-registry could not be cloned");
        }

    }

    private Optional<Mapping> parseMappings(File mappingFile) {
        try {
            return Optional.of(objectMapper.readValue(mappingFile, Mapping.class));
        } catch (Exception e) {
            log.warn(String.format("could not process file %s", mappingFile.getName()), e);
            return Optional.empty();
        }
    }

}
