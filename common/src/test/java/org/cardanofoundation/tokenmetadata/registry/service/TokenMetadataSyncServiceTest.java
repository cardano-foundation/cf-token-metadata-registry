package org.cardanofoundation.tokenmetadata.registry.service;

import org.cardanofoundation.tokenmetadata.registry.entity.OffChainSyncState;
import org.cardanofoundation.tokenmetadata.registry.model.ChangedMappings;
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

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenMetadataSyncServiceTest {

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
    void setUp() {
        tokenMetadataSyncService.isMetadataJobEnabled = true;
        tokenMetadataSyncService.initSyncStatus();
    }

    @Nested
    class SynchronizeDatabase {

        @Test
        void fullSync_whenNoHashStored() {
            when(syncStateRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
            Path mockRepoPath = mock(Path.class);
            File mockMappingsDir = mock(File.class);
            when(mockRepoPath.toFile()).thenReturn(mockMappingsDir);
            when(mockMappingsDir.listFiles()).thenReturn(new File[] {});
            when(gitService.cloneCardanoTokenRegistryGitRepository()).thenReturn(Optional.of(mockRepoPath));
            when(gitService.getHeadCommitHash()).thenReturn(Optional.of(newHash));

            tokenMetadataSyncService.synchronizeDatabase();

            verify(syncStateRepository).save(any(OffChainSyncState.class));
            assertEquals(SyncStatusEnum.SYNC_DONE, tokenMetadataSyncService.getSyncStatus().getStatus());
        }

        @Test
        void incrementalSync_whenHashStoredAndChanged() {
            when(syncStateRepository.findTopByOrderByIdDesc()).thenReturn(Optional.of(new OffChainSyncState(oldHash)));
            Path mockRepoPath = mock(Path.class);
            when(gitService.cloneCardanoTokenRegistryGitRepository()).thenReturn(Optional.of(mockRepoPath));
            when(gitService.getHeadCommitHash()).thenReturn(Optional.of(newHash));

            File mockFile = mock(File.class);
            when(mockFile.getName()).thenReturn("test.json");
            Path mockFilePath = mock(Path.class);
            when(mockFilePath.toFile()).thenReturn(mockFile);
            when(gitService.getChangedMappings(oldHash, newHash))
                    .thenReturn(new ChangedMappings(List.of(mockFilePath), List.of()));

            Mapping mockMapping = new Mapping("test", null, null, null, null, null, null, null);
            when(tokenMappingService.parseMappings(mockFile)).thenReturn(Optional.of(mockMapping));
            when(gitService.getAllMappingDetails(any()))
                    .thenReturn(Map.of("test.json", new MappingUpdateDetails("author", LocalDateTime.now())));
            when(tokenMetadataService.insertMapping(any(), any(), any())).thenReturn(true);

            tokenMetadataSyncService.synchronizeDatabase();

            verify(tokenMetadataService).insertMapping(any(), any(), any());
            verify(tokenMetadataService, never()).deleteMapping(any());
            verify(syncStateRepository).save(any(OffChainSyncState.class));
            assertEquals(SyncStatusEnum.SYNC_DONE, tokenMetadataSyncService.getSyncStatus().getStatus());
        }

        @Test
        void noOp_whenHashesMatch() {
            when(syncStateRepository.findTopByOrderByIdDesc()).thenReturn(Optional.of(new OffChainSyncState(oldHash)));
            Path mockRepoPath = mock(Path.class);
            when(gitService.cloneCardanoTokenRegistryGitRepository()).thenReturn(Optional.of(mockRepoPath));
            when(gitService.getHeadCommitHash()).thenReturn(Optional.of(oldHash));

            tokenMetadataSyncService.synchronizeDatabase();

            verify(tokenMappingService, never()).parseMappings(any());
            verify(syncStateRepository, never()).save(any());
            assertEquals(SyncStatusEnum.SYNC_DONE, tokenMetadataSyncService.getSyncStatus().getStatus());
        }

        @Test
        void error_whenCloneFails() {
            when(syncStateRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
            when(gitService.cloneCardanoTokenRegistryGitRepository()).thenReturn(Optional.empty());

            tokenMetadataSyncService.synchronizeDatabase();

            verify(syncStateRepository, never()).save(any());
            assertEquals(SyncStatusEnum.SYNC_ERROR, tokenMetadataSyncService.getSyncStatus().getStatus());
        }

        @Test
        void fullSync_whenHeadHashUnavailable() {
            when(syncStateRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
            Path mockRepoPath = mock(Path.class);
            File mockMappingsDir = mock(File.class);
            when(mockRepoPath.toFile()).thenReturn(mockMappingsDir);
            when(mockMappingsDir.listFiles()).thenReturn(new File[] {});
            when(gitService.cloneCardanoTokenRegistryGitRepository()).thenReturn(Optional.of(mockRepoPath));
            when(gitService.getHeadCommitHash()).thenReturn(Optional.empty());

            tokenMetadataSyncService.synchronizeDatabase();

            verify(syncStateRepository, never()).save(any());
            assertEquals(SyncStatusEnum.SYNC_DONE, tokenMetadataSyncService.getSyncStatus().getStatus());
        }

        @Test
        void hashNotAdvanced_whenPartialFailure() {
            when(syncStateRepository.findTopByOrderByIdDesc()).thenReturn(Optional.of(new OffChainSyncState(oldHash)));
            Path mockRepoPath = mock(Path.class);
            when(gitService.cloneCardanoTokenRegistryGitRepository()).thenReturn(Optional.of(mockRepoPath));
            when(gitService.getHeadCommitHash()).thenReturn(Optional.of(newHash));

            File mockFile = mock(File.class);
            when(mockFile.getName()).thenReturn("test.json");
            Path mockFilePath = mock(Path.class);
            when(mockFilePath.toFile()).thenReturn(mockFile);
            when(gitService.getChangedMappings(oldHash, newHash))
                    .thenReturn(new ChangedMappings(List.of(mockFilePath), List.of()));

            Mapping mockMapping = new Mapping("test", null, null, null, null, null, null, null);
            when(tokenMappingService.parseMappings(mockFile)).thenReturn(Optional.of(mockMapping));
            when(gitService.getAllMappingDetails(any()))
                    .thenReturn(Map.of("test.json", new MappingUpdateDetails("author", LocalDateTime.now())));
            when(tokenMetadataService.insertMapping(any(), any(), any()))
                    .thenThrow(new RuntimeException("DB error"));

            tokenMetadataSyncService.synchronizeDatabase();

            verify(syncStateRepository, never()).save(any());
            assertEquals(SyncStatusEnum.SYNC_DONE, tokenMetadataSyncService.getSyncStatus().getStatus());
        }

        @Test
        void skipsFile_whenFilenameDoesNotMatchInnerSubject() {
            // Regression: the cardano-token-registry / metadata-registry-testnet
            // repos contain files whose JSON `subject` field does not equal the
            // filename. Indexing them all upserts the same DB row in arbitrary
            // File.listFiles() order, so the same on-chain token gets a different
            // name/description/url depending on which mismatched file came last.
            //
            // Real example: subject baa836fef0... has three files in the testnet
            // registry — baa.../caa.../daa... — each with different content.
            //
            // We accept only the canonical (filename == inner subject) entry so
            // each subject deterministically maps to exactly one file (filenames
            // are unique within a directory).
            when(syncStateRepository.findTopByOrderByIdDesc()).thenReturn(Optional.of(new OffChainSyncState(oldHash)));
            Path mockRepoPath = mock(Path.class);
            when(gitService.cloneCardanoTokenRegistryGitRepository()).thenReturn(Optional.of(mockRepoPath));
            when(gitService.getHeadCommitHash()).thenReturn(Optional.of(newHash));

            // legit.json claims to be the canonical entry for "legit" — accepted.
            File legitFile = mock(File.class);
            when(legitFile.getName()).thenReturn("legit.json");
            Path legitPath = mock(Path.class);
            when(legitPath.toFile()).thenReturn(legitFile);

            // garbage.json *also* claims subject "legit" but its filename is "garbage" — mismatched, must be skipped.
            File garbageFile = mock(File.class);
            when(garbageFile.getName()).thenReturn("garbage.json");
            Path garbagePath = mock(Path.class);
            when(garbagePath.toFile()).thenReturn(garbageFile);

            when(gitService.getChangedMappings(oldHash, newHash))
                    .thenReturn(new ChangedMappings(List.of(legitPath, garbagePath), List.of()));
            when(gitService.getAllMappingDetails(any())).thenReturn(Map.of(
                    "legit.json", new MappingUpdateDetails("author", LocalDateTime.now()),
                    "garbage.json", new MappingUpdateDetails("author", LocalDateTime.now())));
            when(tokenMappingService.parseMappings(legitFile))
                    .thenReturn(Optional.of(new Mapping("legit", null, null, null, null, null, null, null)));
            when(tokenMappingService.parseMappings(garbageFile))
                    .thenReturn(Optional.of(new Mapping("legit", null, null, null, null, null, null, null)));
            when(tokenMetadataService.insertMapping(any(), any(), any())).thenReturn(true);

            tokenMetadataSyncService.synchronizeDatabase();

            // Only one insert: from legit.json. garbage.json is filtered before reaching insertMapping.
            verify(tokenMetadataService, times(1)).insertMapping(any(), any(), any());
            verify(syncStateRepository).save(any(OffChainSyncState.class));
            assertEquals(SyncStatusEnum.SYNC_DONE, tokenMetadataSyncService.getSyncStatus().getStatus());
        }
    }

    @Nested
    class TokenRemovals {

        @Test
        void incrementalSync_deletesTokensRemovedFromRegistry() {
            when(syncStateRepository.findTopByOrderByIdDesc()).thenReturn(Optional.of(new OffChainSyncState(oldHash)));
            Path mockRepoPath = mock(Path.class);
            when(gitService.cloneCardanoTokenRegistryGitRepository()).thenReturn(Optional.of(mockRepoPath));
            when(gitService.getHeadCommitHash()).thenReturn(Optional.of(newHash));

            when(gitService.getChangedMappings(oldHash, newHash))
                    .thenReturn(new ChangedMappings(List.of(), List.of("removedtoken.json")));
            when(tokenMetadataService.deleteMapping("removedtoken")).thenReturn(true);

            tokenMetadataSyncService.synchronizeDatabase();

            verify(tokenMetadataService).deleteMapping("removedtoken");
            verify(tokenMetadataService, never()).insertMapping(any(), any(), any());
            verify(syncStateRepository).save(any(OffChainSyncState.class));
            assertEquals(SyncStatusEnum.SYNC_DONE, tokenMetadataSyncService.getSyncStatus().getStatus());
        }

        @Test
        void hashNotAdvanced_whenDeleteFails() {
            when(syncStateRepository.findTopByOrderByIdDesc()).thenReturn(Optional.of(new OffChainSyncState(oldHash)));
            Path mockRepoPath = mock(Path.class);
            when(gitService.cloneCardanoTokenRegistryGitRepository()).thenReturn(Optional.of(mockRepoPath));
            when(gitService.getHeadCommitHash()).thenReturn(Optional.of(newHash));

            when(gitService.getChangedMappings(oldHash, newHash))
                    .thenReturn(new ChangedMappings(List.of(), List.of("removedtoken.json")));
            when(tokenMetadataService.deleteMapping("removedtoken")).thenReturn(false);

            tokenMetadataSyncService.synchronizeDatabase();

            // Failed deletion must be retried on the next sync, so the commit hash is not advanced.
            verify(syncStateRepository, never()).save(any());
            assertEquals(SyncStatusEnum.SYNC_DONE, tokenMetadataSyncService.getSyncStatus().getStatus());
        }

        @Test
        void fullSync_deletesStaleSubjectsNoLongerInRegistry() {
            when(syncStateRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
            Path mockRepoPath = mock(Path.class);
            File mockMappingsDir = mock(File.class);
            when(mockRepoPath.toFile()).thenReturn(mockMappingsDir);
            File presentFile = mock(File.class);
            when(presentFile.getName()).thenReturn("presenttoken.json");
            when(mockMappingsDir.listFiles()).thenReturn(new File[] {presentFile});
            when(gitService.cloneCardanoTokenRegistryGitRepository()).thenReturn(Optional.of(mockRepoPath));
            when(gitService.getHeadCommitHash()).thenReturn(Optional.of(newHash));

            when(tokenMappingService.parseMappings(presentFile))
                    .thenReturn(Optional.of(new Mapping("presenttoken", null, null, null, null, null, null, null)));
            when(gitService.getAllMappingDetails(any()))
                    .thenReturn(Map.of("presenttoken.json", new MappingUpdateDetails("author", LocalDateTime.now())));
            when(tokenMetadataService.insertMapping(any(), any(), any())).thenReturn(true);
            when(tokenMetadataService.findAllSubjects()).thenReturn(List.of("presenttoken", "staletoken"));
            when(tokenMetadataService.deleteMapping("staletoken")).thenReturn(true);

            tokenMetadataSyncService.synchronizeDatabase();

            verify(tokenMetadataService).deleteMapping("staletoken");
            verify(tokenMetadataService, never()).deleteMapping("presenttoken");
            verify(syncStateRepository).save(any(OffChainSyncState.class));
            assertEquals(SyncStatusEnum.SYNC_DONE, tokenMetadataSyncService.getSyncStatus().getStatus());
        }

        @Test
        void fullSync_skipsStaleCleanup_whenNoMappingFilesFound() {
            when(syncStateRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
            Path mockRepoPath = mock(Path.class);
            File mockMappingsDir = mock(File.class);
            when(mockRepoPath.toFile()).thenReturn(mockMappingsDir);
            when(mockMappingsDir.listFiles()).thenReturn(new File[] {});
            when(gitService.cloneCardanoTokenRegistryGitRepository()).thenReturn(Optional.of(mockRepoPath));
            when(gitService.getHeadCommitHash()).thenReturn(Optional.of(newHash));

            tokenMetadataSyncService.synchronizeDatabase();

            // An empty mappings folder is treated as a broken clone, not a registry
            // where every token was removed — nothing must be deleted.
            verify(tokenMetadataService, never()).findAllSubjects();
            verify(tokenMetadataService, never()).deleteMapping(any());
            assertEquals(SyncStatusEnum.SYNC_DONE, tokenMetadataSyncService.getSyncStatus().getStatus());
        }
    }
}
