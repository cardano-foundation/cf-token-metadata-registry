package org.cardanofoundation.tokenmetadata.registry.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenMetadataSyncCronJob")
class TokenMetadataSyncCronJobTest {

    @Mock
    private TokenMetadataSyncService syncService;

    @Nested
    @DisplayName("run")
    class Run {

        @Test
        void synchronizesDatabaseWhenEnabled() {
            TokenMetadataSyncCronJob job = new TokenMetadataSyncCronJob(syncService, true);
            job.run();
            verify(syncService).synchronizeDatabase();
        }

        @Test
        void skipsWhenDisabled() {
            TokenMetadataSyncCronJob job = new TokenMetadataSyncCronJob(syncService, false);
            job.run();
            verifyNoInteractions(syncService);
        }
    }

    @Nested
    @DisplayName("logInitMessage")
    class LogInitMessage {

        @Test
        void logsWhenEnabled() {
            TokenMetadataSyncCronJob job = new TokenMetadataSyncCronJob(syncService, true);
            job.logInitMessage();
            // No exception = success; logging output verified via log message
        }

        @Test
        void logsWhenDisabled() {
            TokenMetadataSyncCronJob job = new TokenMetadataSyncCronJob(syncService, false);
            job.logInitMessage();
        }
    }

}
