package org.cardanofoundation.metadatatools.api.repository;

import org.cardanofoundation.metadatatools.api.model.data.SubmitToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubmitTokenRepo extends JpaRepository<SubmitToken, String> {
    List<SubmitToken> findAllByUpdatedBy(String updatedBy);
    SubmitToken findBySubject(String subject);
    SubmitToken findBySubjectAndUpdatedBy(String subject, String updatedBy);
}
