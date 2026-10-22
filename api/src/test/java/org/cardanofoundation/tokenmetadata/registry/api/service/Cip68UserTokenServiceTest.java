package org.cardanofoundation.tokenmetadata.registry.api.service;

import org.cardanofoundation.tokenmetadata.registry.entity.Cip68UserToken;
import org.cardanofoundation.tokenmetadata.registry.repository.Cip68UserTokenRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Whether a 333 or 444 user token has been seen (ADR-022): looked up in lower case, a batch in one query. */
class Cip68UserTokenServiceTest {

    private static final String POLICY = "aa".repeat(28);
    private static final String FT_A = POLICY + "0014df1041";
    private static final String RFT_A = POLICY + "001bc28041";

    private final Cip68UserTokenRepository repository = mock(Cip68UserTokenRepository.class);
    private final Cip68UserTokenService service = new Cip68UserTokenService(repository);

    @Test
    void looksATokenUpInLowerCase() {
        when(repository.existsById(FT_A)).thenReturn(true);

        assertThat(service.exists(FT_A.toUpperCase())).isTrue();
        assertThat(service.exists(RFT_A)).isFalse();
    }

    @Test
    void returnsTheSeenTokensOfABatchInLowerCase() {
        when(repository.findAllById(Set.of(FT_A, RFT_A))).thenReturn(List.of(new Cip68UserToken(FT_A, 1L, "tx")));

        assertThat(service.existing(List.of(FT_A.toUpperCase(), RFT_A))).containsExactly(FT_A);
    }

    @Test
    void anEmptyBatchNeedsNoQuery() {
        assertThat(service.existing(List.of())).isEmpty();
        verifyNoInteractions(repository);
    }
}
