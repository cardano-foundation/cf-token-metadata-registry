package org.cardanofoundation.tokenmetadata.registry.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.model.Mapping;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class TokenMappingService {

    private final ObjectMapper objectMapper;

    public Optional<Mapping> parseMappings(File mappingFile) {
        try {
            return Optional.of(objectMapper.readValue(mappingFile, Mapping.class));
        } catch (Exception e) {
            log.warn(String.format("could not process file %s", mappingFile.getName()), e);
            return Optional.empty();
        }
    }

}
