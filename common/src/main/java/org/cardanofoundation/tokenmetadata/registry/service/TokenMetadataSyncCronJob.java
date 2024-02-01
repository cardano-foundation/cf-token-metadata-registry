package org.cardanofoundation.tokenmetadata.registry.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.model.Mapping;
import org.cardanofoundation.tokenmetadata.registry.model.MappingDetails;
import org.cardanofoundation.tokenmetadata.registry.model.MappingUpdateDetails;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Conditional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Service
@Slf4j
@AllArgsConstructor
@ConditionalOnProperty(prefix = "token.metadata.job", value = "enabled", havingValue = "true")
public class TokenMetadataSyncCronJob implements Runnable{

    private final TokenMetadataSyncService tokenMetadataSyncService;

    @Override
    @Scheduled(timeUnit = TimeUnit.MINUTES, initialDelay = 1L, fixedDelay = 60L)
    public void run() {
        tokenMetadataSyncService.synchronizeDatabase();
    }

}
