package org.cardanofoundation.tokenmetadata.registry.invalidtoken;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.entity.InvalidToken;
import org.cardanofoundation.tokenmetadata.registry.repository.InvalidTokenRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Records what is dropped or unusable in {@code invalid_token} (ADR-020), as yaci-store does. It is called from the block
 * transaction of the CIP-68 and CIP-113 listeners, where the input is untrusted and an exception stops the whole sync:
 * <ul>
 *   <li><b>bounded:</b> every text is cut and {@code details} has at most {@value #MAX_DETAILS_CHARS} characters, so an
 *       attacker cannot make a row large;</li>
 *   <li><b>idempotent:</b> the id is a SHA-256 of what identifies the item, and an existing id is left alone, so a replayed
 *       block, a rollback or a re-run sync never duplicates a row and keeps {@code first_seen_at};</li>
 *   <li><b>contained:</b> an exception of its own (building the row, reading or saving it) is a warning, never thrown.</li>
 * </ul>
 * A failure that only shows at flush or commit, inside the database, is not caught here.
 */
@Component
@Slf4j
public class InvalidTokenRecorder {

    static final int MAX_REASON_CHARS = 500;
    static final int MAX_SUBJECT_CHARS = 256;
    static final int MAX_ORIGIN_CHARS = 512;
    static final int MAX_DETAILS_CHARS = 2048;
    static final int MAX_DETAIL_ENTRIES = 16;
    static final int MAX_DETAIL_VALUE_CHARS = 512;

    public static final String COUNTER = "token.metadata.invalid.tokens";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Nullable
    private final InvalidTokenRepository repository;
    private final MeterRegistry registry;
    private final Clock clock;

    @Autowired
    public InvalidTokenRecorder(ObjectProvider<InvalidTokenRepository> repository, ObjectProvider<MeterRegistry> registry,
                                ObjectProvider<Clock> clock) {
        // no repository where the JPA repositories are not created (the "test" profile): nothing is recorded
        this(repository.getIfAvailable(), registry.getIfAvailable(SimpleMeterRegistry::new),
                clock.getIfAvailable(Clock::systemDefaultZone));
    }

    public InvalidTokenRecorder(@Nullable InvalidTokenRepository repository, MeterRegistry registry) {
        this(repository, registry, Clock.systemDefaultZone());
    }

    /** {@code clock} gives {@code first_seen_at}; a fixed one in tests. */
    public InvalidTokenRecorder(@Nullable InvalidTokenRepository repository, MeterRegistry registry, Clock clock) {
        this.repository = repository;
        this.registry = registry;
        this.clock = clock;
    }

    /** A recorder that records nothing, for code built without a database (tests). */
    public static InvalidTokenRecorder noop() {
        return new InvalidTokenRecorder(null, new SimpleMeterRegistry(), Clock.systemDefaultZone());
    }

    /** Records one dropped item, unless the same item is already recorded. Never throws. */
    public void report(InvalidTokenReport report) {
        if (repository == null) {
            return;
        }
        try {
            InvalidToken row = toRow(report);
            if (repository.existsById(row.getId())) {
                return;
            }
            repository.save(row);
            registry.counter(COUNTER, "standard", report.standard(), "reason_code", report.reasonCode()).increment();
        } catch (RuntimeException e) {
            log.warn("Could not record the invalid {} item ({}): {}: {}", report.standard(), report.reasonCode(),
                    e.getClass().getSimpleName(), e.getMessage());
        }
    }

    /** Forgets what an off-chain file had wrong (it is processed again, or was removed). Never throws. */
    public void forgetOrigin(String standard, String origin) {
        if (repository == null) {
            return;
        }
        try {
            repository.deleteByStandardAndOrigin(standard, cut(origin, MAX_ORIGIN_CHARS));
        } catch (RuntimeException e) {
            log.warn("Could not forget the invalid {} items of '{}': {}", standard, origin, e.getMessage());
        }
    }

    InvalidToken toRow(InvalidTokenReport report) {
        return InvalidToken.builder()
                .id(idOf(report))
                .standard(report.standard())
                .combinedWith(report.combinedWith())
                .reasonCode(report.reasonCode())
                .reason(cut(report.reason(), MAX_REASON_CHARS))
                .policyId(report.policyId())
                .assetName(report.assetName())
                .subject(cut(report.subject(), MAX_SUBJECT_CHARS))
                .slot(report.slot())
                .txHash(report.txHash())
                .outputIndex(report.outputIndex())
                .origin(cut(report.origin(), MAX_ORIGIN_CHARS))
                .details(boundedDetails(report.details()))
                .firstSeenAt(LocalDateTime.now(clock))
                .build();
    }

    /** What identifies an item: the same item always gives the same id. */
    public static String idOf(InvalidTokenReport report) {
        String key = String.join("|", report.standard(), report.reasonCode(), nullToEmpty(report.policyId()),
                nullToEmpty(report.assetName()), nullToEmpty(report.txHash()),
                report.outputIndex() == null ? "" : report.outputIndex().toString(),
                nullToEmpty(cut(report.origin(), MAX_ORIGIN_CHARS)));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    @Nullable
    static Map<String, Object> boundedDetails(@Nullable Map<String, Object> details) {
        if (details == null || details.isEmpty()) {
            return null;
        }
        Map<String, Object> bounded = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : details.entrySet()) {
            if (bounded.size() >= MAX_DETAIL_ENTRIES) {
                break;
            }
            Object value = entry.getValue();
            bounded.put(cut(entry.getKey(), 64), value instanceof String text ? cut(text, MAX_DETAIL_VALUE_CHARS) : value);
        }
        try {
            if (MAPPER.writeValueAsString(bounded).length() > MAX_DETAILS_CHARS) {
                return Map.of("truncated", true);
            }
        } catch (JsonProcessingException e) {
            return Map.of("truncated", true);
        }
        return bounded;
    }

    @Nullable
    private static String cut(@Nullable String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    private static String nullToEmpty(@Nullable String value) {
        return value == null ? "" : value;
    }
}
