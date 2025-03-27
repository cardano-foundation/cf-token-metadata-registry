package org.cardanofoundation.tokenmetadata.registry.api.controller;

import com.bloxbean.cardano.client.util.HexUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.api.config.AppConfig;
import org.cardanofoundation.tokenmetadata.registry.api.indexer.V1ApiMetadataIndexer;
import org.cardanofoundation.tokenmetadata.registry.api.model.Pair;
import org.cardanofoundation.tokenmetadata.registry.api.model.QueryPriority;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip68.FungibleTokenMetadata;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.TokenMetadata;
import org.cardanofoundation.tokenmetadata.registry.api.model.v2.*;
import org.cardanofoundation.tokenmetadata.registry.api.service.Cip68FungibleTokenService;
import org.cardanofoundation.tokenmetadata.registry.api.util.AssetType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/v2")
@Slf4j
public class V2ApiController implements V2Api {

    private final AppConfig.CipPriorityConfiguration priorityConfiguration;

    private final Cip68FungibleTokenService cip68FungibleTokenService;

    private final V1ApiMetadataIndexer v1ApiMetadataIndexer;


    private Metadata toMetadata(TokenMetadata metadata) {

        var name = metadata.getName() != null ? new StringProperty(metadata.getName().getValue(), QueryPriority.CIP_26.name()) : null;
        var description = metadata.getDescription() != null ? new StringProperty(metadata.getDescription().getValue(), QueryPriority.CIP_26.name()) : null;
        var ticker = metadata.getTicker() != null ? new StringProperty(metadata.getTicker().getValue(), QueryPriority.CIP_26.name()) : null;
        var decimals = metadata.getDecimals() != null ? new LongProperty(metadata.getDecimals().getValue().longValue(), QueryPriority.CIP_26.name()) : null;
        var logo = metadata.getLogo() != null ? new StringProperty(HexUtil.encodeHexString(metadata.getLogo().getValue()), QueryPriority.CIP_26.name()) : null;
        var url = metadata.getUrl() != null ? new StringProperty(metadata.getUrl().getValue(), QueryPriority.CIP_26.name()) : null;

        return new Metadata(name, description, ticker, decimals, logo, url, null);
    }

    private Metadata toMetadata(FungibleTokenMetadata fungibleTokenMetadata) {

        var name = fungibleTokenMetadata.name() != null ? new StringProperty(fungibleTokenMetadata.name(), QueryPriority.CIP_68.name()) : null;
        var description = fungibleTokenMetadata.description() != null ? new StringProperty(fungibleTokenMetadata.description(), QueryPriority.CIP_68.name()) : null;
        var ticker = fungibleTokenMetadata.ticker() != null ? new StringProperty(fungibleTokenMetadata.ticker(), QueryPriority.CIP_68.name()) : null;
        var decimals = fungibleTokenMetadata.decimals() != null ? new LongProperty(fungibleTokenMetadata.decimals(), QueryPriority.CIP_68.name()) : null;
        var logo = fungibleTokenMetadata.logo() != null ? new StringProperty(fungibleTokenMetadata.logo(), QueryPriority.CIP_68.name()) : null;
        var url = fungibleTokenMetadata.url() != null ? new StringProperty(fungibleTokenMetadata.url(), QueryPriority.CIP_68.name()) : null;
        var version = fungibleTokenMetadata.version() != null ? new LongProperty(fungibleTokenMetadata.version(), QueryPriority.CIP_68.name()) : null;

        return new Metadata(name, description, ticker, decimals, logo, url, version);
    }

    private Optional<Pair<Metadata, Object>> findMetadata(String subject, QueryPriority priority) {
        return switch (priority) {
            case CIP_26 -> v1ApiMetadataIndexer.findSubject(subject)
                    .map(metadata -> new Pair<>(toMetadata(metadata), metadata));
            case CIP_68 -> {
                var assetType = AssetType.fromUnit(subject);
                yield cip68FungibleTokenService.findSubject(assetType.policyId(), assetType.assetName())
                        .map(fungibleTokenMetadata -> new Pair<>(toMetadata(fungibleTokenMetadata), fungibleTokenMetadata));
            }
        };
    }

    @Override
    public ResponseEntity<Response> getSubject(String subject, List<String> properties, List<QueryPriority> priorities) {

        var queryPriority = priorities != null ? priorities : priorityConfiguration.getDefaultPriority();

        var tokenMetadata = queryPriority.stream()
                .reduce(new Pair<>(Metadata.empty(), new ArrayList<>()),
                        (pair, priority) -> findMetadata(subject, priority)
                                .map(metadataObjectPair -> {
                                    pair.second().add(metadataObjectPair.second());
                                    return new Pair<>(pair.first().merge(metadataObjectPair.first()), pair.second());
                                })
                                .orElse(pair)
                        ,
                        (metadataArrayListPair, metadataArrayListPair2) -> {
                            metadataArrayListPair.second().addAll(metadataArrayListPair2.second());
                            var metadata = metadataArrayListPair.first().merge(metadataArrayListPair2.first());
                            return new Pair<>(metadata, metadataArrayListPair.second());
                        });

        if (tokenMetadata.first().equals(Metadata.empty()) || tokenMetadata.second().isEmpty()) {
            return ResponseEntity.notFound().build();
        } else {
            var cip26Opt = tokenMetadata.second().stream().flatMap(object -> {
                if (object instanceof TokenMetadata tokenMetadata1) {
                    return Stream.of(tokenMetadata1);
                } else {
                    return Stream.empty();
                }
            }).findFirst();

            var cip68Opt = tokenMetadata.second().stream().flatMap(object -> {
                if (object instanceof FungibleTokenMetadata fungibleTokenMetadata) {
                    return Stream.of(fungibleTokenMetadata);
                } else {
                    return Stream.empty();
                }
            }).findFirst();

            var standards = new Standards(cip26Opt.orElse(null), cip68Opt.orElse(null));
            var stringPriorities = queryPriority.stream().map(QueryPriority::name).toList();
            var response = new Response(new Subject(subject, tokenMetadata.first(), standards), stringPriorities);
            return ResponseEntity.ok(response);
        }


    }

}
