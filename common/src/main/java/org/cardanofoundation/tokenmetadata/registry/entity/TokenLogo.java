package org.cardanofoundation.tokenmetadata.registry.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Entity
@Table(name = "logo")
@Getter
@Setter
public class TokenLogo {

    /** Matches {@code TokenMetadata.subject} length (CIP-26 spec: 56-120 hex chars). */
    @Id
    @Column(length = 120)
    private String subject;

    /**
     * Image payload associated with the token. Per the CIP-26 specification the canonical form
     * is a <strong>base64-encoded PNG</strong> (spec: {@code image/png} object, &le; 64&nbsp;KB
     * decoded, &le;&nbsp;87400 base64 characters). Real entries in the
     * <a href="https://github.com/cardano-foundation/cardano-token-registry">cardano-token-registry</a>
     * follow this convention — raw base64, no {@code data:} URI prefix.
     * <p>
     * The {@code cf-tokens-cip26} validator enforces the 87&nbsp;400-character length cap only;
     * it does <em>not</em> validate the payload format or MIME type. In principle that means an
     * arbitrary URL (e.g. {@code "https://…/logo.png"}) or a full data URI
     * ({@code "data:image/png;base64,…"}) would also pass validation and be persisted, but such
     * values are off-spec and should not be expected from a compliant registry. Consumers that
     * want to render the logo should attempt base64 decoding first and fall back to treating
     * the value as a URL only if decoding fails.
     */
    private String logo;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TokenLogo tokenLogo = (TokenLogo) o;
        return Objects.equals(subject, tokenLogo.subject);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subject);
    }
}
