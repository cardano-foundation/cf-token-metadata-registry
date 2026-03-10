package org.cardanofoundation.tokenmetadata.registry.service;

import org.cardanofoundation.tokenmetadata.registry.entity.OffChainSyncState;
import org.cardanofoundation.tokenmetadata.registry.model.Mapping;
import org.cardanofoundation.tokenmetadata.registry.model.MappingUpdateDetails;
import org.cardanofoundation.tokenmetadata.registry.model.enums.SyncStatusEnum;
import org.cardanofoundation.tokenmetadata.registry.repository.SyncStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TokenMetadataSyncServiceTest {

    @Mock
    private GitService gitService;

    @Mock
    private TokenMetadataService tokenMetadataService;

    @Mock
    private TokenMappingService tokenMappingService;

    @Mock
    private SyncStateRepository syncStateRepository;

    @InjectMocks
    private TokenMetadataSyncService tokenMetadataSyncService;

    private final String oldHash = "1111111111111111111111111111111111111111";
    private final String newHash = "2222222222222222222222222222222222222222";

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(tokenMetadataSyncService, "isMetadataJobEnabled", true);
        ReflectionTestUtils.invokeMethod(tokenMetadataSyncService, "initSyncStatus");
    }

    @Nested
    public class SynchronizeDatabase {

        @Test
        public void fullSync_whenNoHashStored() throws Exception {
            when(syncStateRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
            Path mockRepoPath = mock(Path.class);
            File mockMappingsDir = mock(File.class);
            when(mockRepoPath.toFile()).thenReturn(mockMappingsDir);
            when(mockMappingsDir.listFiles()).thenReturn(new File[] {});
            when(gitService.cloneCardanoTokenRegistryGitRepository()).thenReturn(Optional.of(mockRepoPath));
            when(gitService.getHeadCommitHash()).thenReturn(Optional.of(newHash));

            tokenMetadataSyncService.synchronizeDatabase();

            verify(syncStateRepository).save(any(OffChainSyncState.class));
            assertEquals(SyncStatusEnum.SYNC_DONE, tokenMetadataSyncService.getSyncStatus().getSyncStatus());
        }

        @Test
        public void incrementalSync_whenHashStoredAndChanged() throws Exception {
            when(syncStateRepository.findTopByOrderByIdDesc()).thenReturn(Optional.of(new OffChainSyncState(oldHash)));
            Path mockRepoPath = mock(Path.class);
            when(gitService.cloneCardanoTokenRegistryGitRepository()).thenReturn(Optional.of(mockRepoPath));
            when(gitService.getHeadCommitHash()).thenReturn(Optional.of(newHash));

            File mockFile = mock(File.class);
            Path mockFilePath = mock(Path.class);
            when(mockFilePath.toFile()).thenReturn(mockFile);
            when(gitService.getChangedFiles(oldHash, newHash)).thenReturn(List.of(mockFilePath));

            Mapping mockMapping = new Mapping("test", null, null, null, null, null, null, null);
            when(tokenMappingService.parseMappings(mockFile)).thenReturn(Optional.of(mockMapping));
            when(gitService.getMappingDetails(mockFile))
                    .thenReturn(Optional.of(new MappingUpdateDetails("author", LocalDateTime.now())));
            when(tokenMetadataService.insertMapping(any(), any(), any())).thenReturn(true);

            tokenMetadataSyncService.synchronizeDatabase();

            verify(tokenMetadataService).insertMapping(any(), any(), any());
            verify(gitService, never()).getMappingDetails(argThat(f -> !f.equals(mockFile)));
            verify(syncStateRepository).save(any(OffChainSyncState.class));
            assertEquals(SyncStatusEnum.SYNC_DONE, tokenMetadataSyncService.getSyncStatus().getSyncStatus());
        }

        @Test
        public void noOp_whenHashesMatch() {
            when(syncStateRepository.findTopByOrderByIdDesc()).thenReturn(Optional.of(new OffChainSyncState(oldHash)));
            Path mockRepoPath = mock(Path.class);
            when(gitService.cloneCardanoTokenRegistryGitRepository()).thenReturn(Optional.of(mockRepoPath));
            when(gitService.getHeadCommitHash()).thenReturn(Optional.of(oldHash));

            tokenMetadataSyncService.synchronizeDatabase();

            verify(tokenMappingService, never()).parseMappings(any());
            verify(syncStateRepository, never()).save(any());
            assertEquals(SyncStatusEnum.SYNC_DONE, tokenMetadataSyncService.getSyncStatus().getSyncStatus());
        }

        @Test
        public void error_whenCloneFails() {
            when(syncStateRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
            when(gitService.cloneCardanoTokenRegistryGitRepository()).thenReturn(Optional.empty());

            tokenMetadataSyncService.synchronizeDatabase();

            verify(syncStateRepository, never()).save(any());
            assertEquals(SyncStatusEnum.SYNC_ERROR, tokenMetadataSyncService.getSyncStatus().getSyncStatus());
        }
    }
}
