package org.cardanofoundation.tokenmetadata.registry.api.service.cip113;

import lombok.RequiredArgsConstructor;
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
 * whose tokens have none cannot be served: it gets one row, and the row is removed as soon as one of its tokens has
 * CIP-26 or CIP-68 metadata (with or without validation errors), or the policy is no longer registered.
 * <p>
 * The registry indexes the registry nodes, not the mints or the programmable-token ("jail") address, so this is known
 * per policy, not per token, and a policy registered with nothing minted yet is reported the same way.
 * <p>
 * It is a state, not an event, so it is recomputed: after registry nodes are indexed, after a rollback, and on a
 * schedule that picks up the CIP-26 sync and new CIP-68 datums. The work is three small queries and touches only the
 * rows that change; a failure is a warning, never thrown, since it can run inside the block transaction.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class Cip113CompanionReconciler {

    private final Cip113Configuration cip113Configuration;
    private final Cip113RegistryNodeRepository cip113RegistryNodeRepository;
    private final TokenMetadataRepository tokenMetadataRepository;
    private final MetadataReferenceNftRepository metadataReferenceNftRepository;
    private final InvalidTokenRepository invalidTokenRepository;
    private final InvalidTokenRecorder invalidTokenRecorder;

    @Scheduled(fixedDelayString = "${cip113.companion.reconcile.interval-ms:600000}",
            initialDelayString = "${cip113.companion.reconcile.initial-delay-ms:120000}")
    @Transactional
    public void reconcileOnSchedule() {
        reconcile();
    }

    /** Recomputes the rows. Never throws. */
    public void reconcile() {
        if (!cip113Configuration.isEnabled()) {
            return;
        }
        try {
            Set<String> registered = new HashSet<>(cip113RegistryNodeRepository.findRegisteredPolicyIds());
            Set<String> withCompanion = new HashSet<>();
            if (!registered.isEmpty()) {
                withCompanion.addAll(tokenMetadataRepository.findPolicyIdsWithMetadata(registered));
                withCompanion.addAll(metadataReferenceNftRepository.findPolicyIdsWithMetadata(registered));
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
