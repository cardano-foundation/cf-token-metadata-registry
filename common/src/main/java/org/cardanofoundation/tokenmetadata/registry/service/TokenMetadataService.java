package org.cardanofoundation.tokenmetadata.registry.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.model.Mapping;
import org.cardanofoundation.tokenmetadata.registry.repository.TokenLogoRepository;
import org.cardanofoundation.tokenmetadata.registry.repository.TokenMetadataRepository;
import org.cardanofoundation.tokenmetadata.registry.util.MappingsUtil;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

import static org.cardanofoundation.tokenmetadata.registry.util.MappingsUtil.toTokenLogo;

@Component
@Slf4j
@AllArgsConstructor
public class TokenMetadataService {


    private final TokenMetadataRepository tokenMetadataRepository;

    private final TokenLogoRepository tokenLogoRepository;

    public void insertMapping(Mapping mapping, LocalDateTime updatedAt, String updateBy) {
        var tokenMetadata = MappingsUtil.toTokenMetadata(mapping, updateBy, updatedAt);
        tokenMetadataRepository.save(tokenMetadata);
    }

    public void insertLogo(Mapping mapping) {
        var tokenLogo = toTokenLogo(mapping);
        tokenLogoRepository.save(tokenLogo);
    }

}
