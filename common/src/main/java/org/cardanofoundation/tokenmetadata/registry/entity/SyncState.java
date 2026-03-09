package org.cardanofoundation.tokenmetadata.registry.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sync_state")
@Getter
@Setter
@NoArgsConstructor
public class SyncState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "last_commit_hash", length = 40, nullable = false)
    private String lastCommitHash;

    public SyncState(String lastCommitHash) {
        this.lastCommitHash = lastCommitHash;
    }
}
