package org.cardanofoundation.tokenmetadata.registry.api.service;

import lombok.RequiredArgsConstructor;
import org.cardanofoundation.tokenmetadata.registry.entity.Cip68UserToken;
import org.cardanofoundation.tokenmetadata.registry.repository.Cip68UserTokenRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Answers whether a CIP-68 fungible (333) or rich fungible (444) user token has been seen on chain (ADR-022). The V2 API
 * serves the CIP-68 metadata of such a subject only when it has: the reference NFT of the same name holds the metadata,
 * but it does not say that the user token was ever minted. Ported from yaci-store (bloxbean/yaci-store#1294).
 */
@Service
@RequiredArgsConstructor
public class Cip68UserTokenService {

    private final Cip68UserTokenRepository repository;

    public boolean exists(String unit) {
        return repository.existsById(normalize(unit));
    }

    /** The units of {@code units} that have been seen, in the same normal form (lower case) as asked for. One query. */
    public Set<String> existing(Collection<String> units) {
        if (units.isEmpty()) {
            return Set.of();
        }
        Set<String> wanted = units.stream().map(Cip68UserTokenService::normalize).collect(Collectors.toCollection(HashSet::new));
        return repository.findAllById(wanted).stream().map(Cip68UserToken::getUnit).collect(Collectors.toSet());
    }

    public static String normalize(String unit) {
        return unit.toLowerCase(Locale.ROOT);
    }
}
