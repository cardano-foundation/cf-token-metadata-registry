package org.cardanofoundation.tokenmetadata.registry.job.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.job.model.Mapping;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class MappingParsingTest {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testMappingDeserialization() {

        File mappings = Path.of("/tmp/cardano-token-registry/mappings").toFile();
        var mappinsFiles = Arrays.asList(mappings.listFiles());
        var counter = new AtomicLong(0L);
        mappinsFiles.forEach(file -> {

            try {
                objectMapper.readValue(file, Mapping.class);
            } catch (IOException e) {
                counter.incrementAndGet();
                log.warn(file.getName() + " could not be deserialised", e);
            }

        });

        log.info("Total {}, broken: {}", mappinsFiles.size(), counter.get());

    }

}
