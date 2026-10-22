package org.cardanofoundation.tokenmetadata.registry.api.service.cip113;

import lombok.extern.slf4j.Slf4j;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Keeps the {@code no_companion_metadata} rows of {@code invalid_token} (ADR-020) in step with the data. A programmable
 * token (CIP-113) is served only with its CIP-26 or CIP-68 metadata, so a policy registered in the CIP-113 registry
 * whose tokens have none that is served cannot be served: it gets one row, and the row is removed as soon as one of its
 * tokens has a CIP-26 entry or served CIP-68 metadata (a reference NFT whose 333 or 444 token was minted, ADR-022), with
 * or without validation errors, or the policy is no longer registered.
 * <p>
 * The registry indexes the registry nodes, not the mints or the programmable-token ("jail") address, so this is known
 * per policy, not per token, and a policy registered with nothing minted yet is reported the same way.
 * <p>
 * It is a state, not an event, so it is recomputed when its inputs change:
 * <ul>
 *   <li>registry nodes are indexed or rolled back ({@code Cip113EventListener});</li>
 *   <li>the CIP-68 listener stores a reference NFT or sees a new user token of a registered policy
 *       ({@link #reconcileIfRegistered});</li>
 *   <li>a CIP-26 sync run in this process changes entries ({@link Cip26SyncCompletedEvent});</li>
 *   <li>the application starts.</li>
 * </ul>
 * When the CIP-26 sync runs in a separate {@code job} process ({@code token.metadata.job.enabled=false}), its changes
 * reach this process only through the database, so it also runs on a schedule
 * ({@code cip113.companion.reconcile.interval-ms}, 10 minutes by default). A read-only instance never reconciles: it does
 * not write. The work is three small queries and touches only the rows that change; a failure is a warning, never
 * thrown, since it can run inside the block transaction.
 */
@Service
@Slf4j
public class Cip113CompanionReconciler {

    private final Cip113Configuration cip113Configuration;
    private final Cip113RegistryNodeRepository cip113RegistryNodeRepository;
    private final TokenMetadataRepository tokenMetadataRepository;
    private final MetadataReferenceNftRepository metadataReferenceNftRepository;
    private final InvalidTokenRepository invalidTokenRepository;
    private final InvalidTokenRecorder invalidTokenRecorder;
    private final boolean cip26SyncInThisProcess;
    private final boolean readOnly;

    public Cip113CompanionReconciler(Cip113Configuration cip113Configuration,
                                     Cip113RegistryNodeRepository cip113RegistryNodeRepository,
                                     TokenMetadataRepository tokenMetadataRepository,
                                     MetadataReferenceNftRepository metadataReferenceNftRepository,
                                     InvalidTokenRepository invalidTokenRepository,
                                     InvalidTokenRecorder invalidTokenRecorder,
                                     @Value("${token.metadata.job.enabled:false}") boolean cip26SyncInThisProcess,
                                     @Value("${store.read-only-mode:false}") boolean readOnly) {
        this.cip113Configuration = cip113Configuration;
        this.cip113RegistryNodeRepository = cip113RegistryNodeRepository;
        this.tokenMetadataRepository = tokenMetadataRepository;
        this.metadataReferenceNftRepository = metadataReferenceNftRepository;
        this.invalidTokenRepository = invalidTokenRepository;
        this.invalidTokenRecorder = invalidTokenRecorder;
        this.cip26SyncInThisProcess = cip26SyncInThisProcess;
        this.readOnly = readOnly;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void reconcileOnStartup() {
        reconcile();
    }

    @EventListener
    @Transactional
    public void reconcileAfterCip26Sync(Cip26SyncCompletedEvent event) {
        reconcile();
    }

    /** Only when the CIP-26 sync runs in another process: its changes cannot be seen any other way. */
    @Scheduled(fixedDelayString = "${cip113.companion.reconcile.interval-ms:600000}",
            initialDelayString = "${cip113.companion.reconcile.initial-delay-ms:120000}")
    @Transactional
    public void reconcileOnSchedule() {
        if (!cip26SyncInThisProcess) {
            reconcile();
        }
    }

    /**
     * Recomputes the rows if one of {@code policyIds} is registered in the CIP-113 registry: the CIP-68 listener calls
     * it with the policies of the reference NFTs it stored and of the user tokens it saw for the first time. Never throws.
     */
    public void reconcileIfRegistered(Set<String> policyIds) {
        if (policyIds.isEmpty() || !cip113Configuration.isEnabled() || readOnly) {
            return;
        }
        try {
            if (cip113RegistryNodeRepository.findRegisteredPolicyIds().stream().anyMatch(policyIds::contains)) {
                reconcile();
            }
        } catch (RuntimeException e) {
            log.warn("Could not check the CIP-113 registration of {} policies: {}: {}", policyIds.size(),
                    e.getClass().getSimpleName(), e.getMessage());
        }
    }

    /** Recomputes the rows. Never throws. */
    public void reconcile() {
        if (!cip113Configuration.isEnabled() || readOnly) {
            return;
        }
        try {
            Set<String> registered = new HashSet<>(cip113RegistryNodeRepository.findRegisteredPolicyIds());
            Set<String> withCompanion = new HashSet<>();
            if (!registered.isEmpty()) {
                withCompanion.addAll(tokenMetadataRepository.findPolicyIdsWithMetadata(registered));
                withCompanion.addAll(metadataReferenceNftRepository.findPolicyIdsWithServedMetadata(registered));
            }
            Set<String> withoutCompanion = registered.stream()
                    .filter(policyId -> !withCompanion.contains(policyId))
                    .collect(Collectors.toSet());

            List<InvalidToken> existing = invalidTokenRepository.findByStandardAndReasonCode(
                    InvalidTokenReason.CIP_113, InvalidTokenReason.NO_COMPANION_METADATA);
            Set<String> recorded = existing.stream().map(InvalidToken::getPolicyId).collect(Collectors.toSet());

            List<String> stale = existing.stream()
                    .filter(row -> !withoutCompanion.contains(row.getPolicyId()))
                    .map(InvalidToken::getId)
                    .toList();
            if (!stale.isEmpty()) {
                invalidTokenRepository.deleteByIdIn(stale);
            }
            withoutCompanion.stream()
                    .filter(policyId -> !recorded.contains(policyId))
                    .forEach(policyId -> invalidTokenRecorder.report(report(policyId)));

            if (!stale.isEmpty() || withoutCompanion.size() != recorded.size()) {
                log.info("CIP-113: {} of {} registered policies have no CIP-26 or CIP-68 metadata ({} rows removed)",
                        withoutCompanion.size(), registered.size(), stale.size());
            }
        } catch (RuntimeException e) {
            log.warn("Could not reconcile the CIP-113 policies without companion metadata: {}: {}",
                    e.getClass().getSimpleName(), e.getMessage());
        }
    }

    static InvalidTokenReport report(String policyId) {
        return InvalidTokenReport.builder()
                .standard(InvalidTokenReason.CIP_113)
                .reasonCode(InvalidTokenReason.NO_COMPANION_METADATA)
                .reason("policy " + policyId + " is registered in the CIP-113 registry, but none of its tokens has CIP-26 "
                        + "or CIP-68 metadata, so none of them can be served")
                .policyId(policyId)
                .details(Map.of("registry_key", policyId))
                .build();
    }
}
