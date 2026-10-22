package org.cardanofoundation.tokenmetadata.registry.it;

import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.backend.api.BackendService;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.core.ConditionTimeoutException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;

import static org.awaitility.Awaitility.await;

/**
 * Funds a test wallet from the Yaci DevKit faucet and waits until the funds can be spent.
 * <p>
 * The faucet answers 2xx as soon as it accepted the request, but its transaction sometimes takes long to appear, or
 * never does: CI runs where every minting test waited 30 s for funds that never came made whole runs fail. So the
 * faucet is asked again when the funds do not show up, a few times, before giving up.
 */
@Slf4j
final class DevKitFaucet {

    private static final String TOPUP_URL = "http://localhost:10000/local-cluster/api/addresses/topup";
    private static final long ADA = 50_000;
    private static final int ATTEMPTS = 3;
    private static final Duration WAIT_PER_ATTEMPT = Duration.ofSeconds(40);

    private DevKitFaucet() {
    }

    static void fund(BackendService backendService, RestTemplate restTemplate, String address) {
        for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {
            topUp(restTemplate, address);
            try {
                await().atMost(WAIT_PER_ATTEMPT)
                        .pollInterval(Duration.ofSeconds(2))
                        .ignoreExceptions()
                        .until(() -> isFunded(backendService, address));
                log.info("Funds of {} available (attempt {})", address, attempt);
                return;
            } catch (ConditionTimeoutException e) {
                log.warn("Funds of {} not available after {} s (attempt {} of {}), asking the faucet again", address,
                        WAIT_PER_ATTEMPT.toSeconds(), attempt, ATTEMPTS);
            }
        }
        throw new IllegalStateException("the DevKit faucet did not fund " + address + " after " + ATTEMPTS + " attempts");
    }

    private static boolean isFunded(BackendService backendService, String address) throws Exception {
        Result<List<Utxo>> utxos = backendService.getUtxoService().getUtxos(address, 1, 1);
        return utxos.isSuccessful() && !utxos.getValue().isEmpty();
    }

    private static void topUp(RestTemplate restTemplate, String address) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String json = String.format("{\"address\": \"%s\", \"adaAmount\": %d}", address, ADA);
            ResponseEntity<String> response = restTemplate.postForEntity(TOPUP_URL, new HttpEntity<>(json, headers), String.class);
            log.info("Faucet answered {} for {}", response.getStatusCode(), address);
        } catch (Exception e) {
            log.warn("Could not top up {}: {}", address, e.getMessage());
        }
    }
}
