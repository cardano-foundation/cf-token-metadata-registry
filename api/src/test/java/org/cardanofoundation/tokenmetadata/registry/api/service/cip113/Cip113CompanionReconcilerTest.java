package org.cardanofoundation.tokenmetadata.registry.api.service.cip113;

import org.cardanofoundation.tokenmetadata.registry.api.config.Cip113Configuration;
import org.cardanofoundation.tokenmetadata.registry.entity.InvalidToken;
import org.cardanofoundation.tokenmetadata.registry.invalidtoken.InvalidTokenReason;
import org.cardanofoundation.tokenmetadata.registry.invalidtoken.InvalidTokenRecorder;
import org.cardanofoundation.tokenmetadata.registry.invalidtoken.InvalidTokenReport;
import org.cardanofoundation.tokenmetadata.registry.repository.Cip113RegistryNodeRepository;
import org.cardanofoundation.tokenmetadata.registry.repository.InvalidTokenRepository;
import org.cardanofoundation.tokenmetadata.registry.repository.MetadataReferenceNftRepository;
import org.cardanofoundation.tokenmetadata.registry.repository.TokenMetadataRepository;
import org.cardanofoundation.tokenmetadata.registry.service.Cip26SyncCompletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.cardanofoundation.tokenmetadata.registry.api.event.Cip68MetadataIndexedEvent;
import org.cardanofoundation.tokenmetadata.registry.api.event.Cip113RegistryChangedEvent;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * A policy registered in the CIP-113 registry whose tokens have no CIP-26 or CIP-68 metadata cannot be served: it has one
 * {@code no_companion_metadata} row in {@code invalid_token}, kept in step with the data (ADR-020).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Cip113CompanionReconciler")
class Cip113CompanionReconcilerTest {

    private static final String REGISTRY_POLICY = "3083d387537f9318b6ff5aeab7de5f5629d26495319b8fafd409222a";
    private static final String WITH_CIP26 = "11111111111111111111111111111111111111111111111111111111";
    private static final String WITH_CIP68 = "22222222222222222222222222222222222222222222222222222222";
    private static final String WITHOUT = "33333333333333333333333333333333333333333333333333333333";

    @Mock
    private Cip113RegistryNodeRepository registryNodeRepository;
    @Mock
    private TokenMetadataRepository tokenMetadataRepository;
    @Mock
    private MetadataReferenceNftRepository metadataReferenceNftRepository;
    @Mock
    private InvalidTokenRepository invalidTokenRepository;
    @Mock
    private InvalidTokenRecorder invalidTokenRecorder;

    private Cip113Configuration configuration;
    private Cip113CompanionReconciler reconciler;

    @BeforeEach
    void setUp() {
        configuration = new Cip113Configuration();
        configuration.setRegistryNftPolicyIds(List.of(REGISTRY_POLICY));
        configuration.init();
        reconciler = reconciler(true, false);
    }

    private Cip113CompanionReconciler reconciler(boolean cip26SyncInThisProcess, boolean readOnly) {
        return new Cip113CompanionReconciler(configuration, registryNodeRepository, tokenMetadataRepository,
                metadataReferenceNftRepository, invalidTokenRepository, invalidTokenRecorder, cip26SyncInThisProcess, readOnly);
    }

    /** It is recomputed when its inputs change, and on a schedule only when the CIP-26 sync runs in another process. */
    @Nested
    @DisplayName("When it runs")
    class Triggers {

        private void nothingRegistered() {
            when(registryNodeRepository.findRegisteredPolicyIds()).thenReturn(List.of());
            when(invalidTokenRepository.findByStandardAndReasonCode(InvalidTokenReason.CIP_113, InvalidTokenReason.NO_COMPANION_METADATA))
                    .thenReturn(List.of());
        }

        @Test
        void afterACip26SyncRunInThisProcess() {
            nothingRegistered();

            reconciler.onCip26SyncCompleted(new Cip26SyncCompletedEvent(3, 0));

            verify(invalidTokenRepository).findByStandardAndReasonCode(InvalidTokenReason.CIP_113, InvalidTokenReason.NO_COMPANION_METADATA);
        }

        @Test
        void whenTheCip113RegistryChanged() {
            nothingRegistered();

            reconciler.onCip113RegistryChanged(Cip113RegistryChangedEvent.indexed(1));

            verify(invalidTokenRepository).findByStandardAndReasonCode(InvalidTokenReason.CIP_113, InvalidTokenReason.NO_COMPANION_METADATA);
        }

        /**
         * The events are published inside Yaci Store's block transaction; the reconciler must run after it commits, in a
         * transaction of its own, so that a database error here cannot abort the block and stop the sync.
         */
        @ParameterizedTest
        @ValueSource(strings = {"onCip26SyncCompleted", "onCip113RegistryChanged", "onCip68MetadataIndexed"})
        void reactsAfterThePublishingTransactionCommitsInATransactionOfItsOwn(String handler) {
            Method method = Arrays.stream(Cip113CompanionReconciler.class.getMethods())
                    .filter(m -> m.getName().equals(handler)).findFirst().orElseThrow();

            TransactionalEventListener listener = method.getAnnotation(TransactionalEventListener.class);
            assertThat(listener.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
            assertThat(listener.fallbackExecution()).as("also delivered when published outside a transaction").isTrue();
            assertThat(method.getAnnotation(Transactional.class).propagation()).isEqualTo(Propagation.REQUIRES_NEW);
        }

        @Test
        void onStartup() {
            nothingRegistered();

            reconciler.reconcileOnStartup();

            verify(invalidTokenRepository).findByStandardAndReasonCode(InvalidTokenReason.CIP_113, InvalidTokenReason.NO_COMPANION_METADATA);
        }

        @Test
        void notOnTheScheduleWhenTheCip26SyncRunsInThisProcess() {
            reconciler.reconcileOnSchedule();

            verifyNoInteractions(registryNodeRepository, invalidTokenRepository);
        }

        @Test
        void onTheScheduleWhenTheCip26SyncRunsInAnotherProcess() {
            nothingRegistered();

            reconciler(false, false).reconcileOnSchedule();

            verify(invalidTokenRepository).findByStandardAndReasonCode(InvalidTokenReason.CIP_113, InvalidTokenReason.NO_COMPANION_METADATA);
        }

        @Test
        void whenTheCip68ListenerSeesMetadataOfARegisteredPolicy() {
            when(registryNodeRepository.findRegisteredPolicyIds()).thenReturn(List.of(WITHOUT));
            when(invalidTokenRepository.findByStandardAndReasonCode(InvalidTokenReason.CIP_113, InvalidTokenReason.NO_COMPANION_METADATA))
                    .thenReturn(List.of());

            reconciler.onCip68MetadataIndexed(new Cip68MetadataIndexedEvent(Set.of(WITHOUT, WITH_CIP68)));

            verify(invalidTokenRepository).findByStandardAndReasonCode(InvalidTokenReason.CIP_113, InvalidTokenReason.NO_COMPANION_METADATA);
        }

        @Test
        void notWhenTheCip68ListenerSeesMetadataOfPoliciesThatAreNotRegistered() {
            when(registryNodeRepository.findRegisteredPolicyIds()).thenReturn(List.of(WITHOUT));

            reconciler.onCip68MetadataIndexed(new Cip68MetadataIndexedEvent(Set.of(WITH_CIP68)));

            verifyNoInteractions(invalidTokenRepository);
        }

        @Test
        void notForABlockWithoutCip68Metadata() {
            reconciler.onCip68MetadataIndexed(new Cip68MetadataIndexedEvent(Set.of()));

            verifyNoInteractions(registryNodeRepository, invalidTokenRepository);
        }

        @Test
        void neverOnAReadOnlyInstance() {
            Cip113CompanionReconciler readOnly = reconciler(false, true);

            readOnly.reconcile();
            readOnly.reconcileOnSchedule();
            readOnly.onCip68MetadataIndexed(new Cip68MetadataIndexedEvent(Set.of(WITHOUT)));
            readOnly.onCip113RegistryChanged(Cip113RegistryChangedEvent.indexed(1));

            verifyNoInteractions(registryNodeRepository, invalidTokenRepository, invalidTokenRecorder);
        }
    }

    @Test
    void recordsEveryRegisteredPolicyWithoutCip26OrCip68Metadata() {
        when(registryNodeRepository.findRegisteredPolicyIds()).thenReturn(List.of(WITH_CIP26, WITH_CIP68, WITHOUT));
        when(tokenMetadataRepository.findPolicyIdsWithMetadata(anyCollection())).thenReturn(List.of(WITH_CIP26));
        when(metadataReferenceNftRepository.findPolicyIdsWithMetadata(anyCollection())).thenReturn(List.of(WITH_CIP68));
        when(invalidTokenRepository.findByStandardAndReasonCode(InvalidTokenReason.CIP_113, InvalidTokenReason.NO_COMPANION_METADATA))
                .thenReturn(List.of());

        reconciler.reconcile();

        ArgumentCaptor<InvalidTokenReport> report = ArgumentCaptor.forClass(InvalidTokenReport.class);
        verify(invalidTokenRecorder).report(report.capture());
        assertThat(report.getValue().standard()).isEqualTo(InvalidTokenReason.CIP_113);
        assertThat(report.getValue().reasonCode()).isEqualTo(InvalidTokenReason.NO_COMPANION_METADATA);
        assertThat(report.getValue().policyId()).isEqualTo(WITHOUT);
        assertThat(report.getValue().slot()).isNull();
        verify(invalidTokenRepository, never()).deleteByIdIn(anyCollection());
    }

    @Test
    void removesTheRowOfAPolicyThatNowHasMetadataAndKeepsTheOthers() {
        when(registryNodeRepository.findRegisteredPolicyIds()).thenReturn(List.of(WITH_CIP68, WITHOUT));
        when(tokenMetadataRepository.findPolicyIdsWithMetadata(anyCollection())).thenReturn(List.of());
        when(metadataReferenceNftRepository.findPolicyIdsWithMetadata(anyCollection())).thenReturn(List.of(WITH_CIP68));
        when(invalidTokenRepository.findByStandardAndReasonCode(InvalidTokenReason.CIP_113, InvalidTokenReason.NO_COMPANION_METADATA))
                .thenReturn(List.of(row("id-cip68", WITH_CIP68), row("id-without", WITHOUT)));

        reconciler.reconcile();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<String>> removed = ArgumentCaptor.forClass(Collection.class);
        verify(invalidTokenRepository).deleteByIdIn(removed.capture());
        assertThat(removed.getValue()).containsExactly("id-cip68");
        // the row of the policy still without metadata is already there: nothing new is written
        verify(invalidTokenRecorder, never()).report(any());
    }

    @Test
    void removesTheRowOfAPolicyThatIsNoLongerRegistered() {
        when(registryNodeRepository.findRegisteredPolicyIds()).thenReturn(List.of());
        when(invalidTokenRepository.findByStandardAndReasonCode(InvalidTokenReason.CIP_113, InvalidTokenReason.NO_COMPANION_METADATA))
                .thenReturn(List.of(row("id-gone", WITHOUT)));

        reconciler.reconcile();

        verify(invalidTokenRepository).deleteByIdIn(List.of("id-gone"));
        verifyNoInteractions(tokenMetadataRepository, metadataReferenceNftRepository);
    }

    @Test
    void doesNothingWhenCip113IsNotEnabled() {
        Cip113Configuration disabled = new Cip113Configuration();
        disabled.setRegistryNftPolicyIds(List.of());
        disabled.init();

        new Cip113CompanionReconciler(disabled, registryNodeRepository, tokenMetadataRepository, metadataReferenceNftRepository,
                invalidTokenRepository, invalidTokenRecorder, true, false).reconcile();

        verifyNoInteractions(registryNodeRepository, invalidTokenRepository, invalidTokenRecorder);
    }

    @Test
    void neverThrowsSinceItCanRunInsideTheBlockTransaction() {
        when(registryNodeRepository.findRegisteredPolicyIds()).thenThrow(new IllegalStateException("database down"));

        assertThatCode(reconciler::reconcile).doesNotThrowAnyException();
    }

    private static InvalidToken row(String id, String policyId) {
        return InvalidToken.builder().id(id).standard(InvalidTokenReason.CIP_113)
                .reasonCode(InvalidTokenReason.NO_COMPANION_METADATA).reason("r").policyId(policyId).build();
    }
}
