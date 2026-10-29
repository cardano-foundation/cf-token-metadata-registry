package org.cardanofoundation.tokenmetadata.registry.invalidtoken;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.cardanofoundation.tokenmetadata.registry.config.RepositoryTestConfig;
import org.cardanofoundation.tokenmetadata.registry.entity.InvalidToken;
import org.cardanofoundation.tokenmetadata.registry.repository.InvalidTokenRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The recorder against a real database, called the way the CIP-26 sync calls it: with no transaction of its own. The
 * class is deliberately not {@code @Transactional}: a test transaction would hide a delete query that needs one, which
 * is how {@link InvalidTokenRecorder#forgetOrigin} failed on every file of a mainnet sync while the tests passed.
 */
@SpringBootTest(classes = RepositoryTestConfig.class)
@AutoConfigureTestDatabase
@ActiveProfiles("integration-test")
class InvalidTokenRecorderIT {

    @Autowired
    private InvalidTokenRepository repository;
    private InvalidTokenRecorder recorder;

    @BeforeEach
    void setUp() {
        recorder = new InvalidTokenRecorder(repository, new SimpleMeterRegistry());
    }

    @AfterEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Test
    void forgetsTheInvalidItemsOfAFileOutsideATransaction() {
        repository.saveAll(List.of(
                invalid("fixed-file", "CIP-26", "garbage.json"),
                invalid("other-file", "CIP-26", "other.json"),
                invalid("same-origin-other-standard", "CIP-68", "garbage.json")));

        recorder.forgetOrigin("CIP-26", "garbage.json");

        assertThat(repository.findAll()).extracting(InvalidToken::getId)
                .containsExactlyInAnyOrder("other-file", "same-origin-other-standard");
    }

    @Test
    void forgetsARollbackOutsideATransaction() {
        repository.saveAll(List.of(
                invalid("before-rollback", 100L),
                invalid("after-rollback", 200L)));

        assertThat(repository.deleteBySlotGreaterThan(150L)).isEqualTo(1);

        assertThat(repository.findAll()).extracting(InvalidToken::getId).containsExactly("before-rollback");
    }

    private static InvalidToken invalid(String id, String standard, String origin) {
        return InvalidToken.builder().id(id).standard(standard).reasonCode("r").reason("r").origin(origin).build();
    }

    private static InvalidToken invalid(String id, Long slot) {
        return InvalidToken.builder().id(id).standard("CIP-68").reasonCode("r").reason("r").slot(slot).build();
    }
}
