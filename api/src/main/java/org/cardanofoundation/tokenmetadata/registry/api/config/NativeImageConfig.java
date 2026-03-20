package org.cardanofoundation.tokenmetadata.registry.api.config;

import org.cardanofoundation.tokenmetadata.registry.api.model.rest.AnnotatedSignature;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.BatchRequest;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.BatchResponse;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.FilterOperand;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.HealthResponse;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.PivotDirection;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.SubjectsResponse;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.TokenMetadataProperty;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.VerifyFailureResponse;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.WalletFraudIncident;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.WalletHashes;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.WalletTrustCheckResponse;
import org.cardanofoundation.tokenmetadata.registry.entity.MetadataReferenceNft;
import org.cardanofoundation.tokenmetadata.registry.entity.MetadataReferenceNftId;
import org.cardanofoundation.tokenmetadata.registry.entity.OffChainSyncState;
import org.cardanofoundation.tokenmetadata.registry.entity.TokenLogo;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

import static org.springframework.aot.hint.MemberCategory.*;

@Configuration(proxyBeanMethods = false)
@ImportRuntimeHints(NativeImageConfig.Hints.class)
public class NativeImageConfig {

    private NativeImageConfig() {
    }

    static class Hints implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            var reflection = hints.reflection();

            // JPA entities
            for (var clazz : new Class<?>[]{
                    org.cardanofoundation.tokenmetadata.registry.entity.TokenMetadata.class,
                    TokenLogo.class,
                    MetadataReferenceNft.class,
                    MetadataReferenceNftId.class,
                    OffChainSyncState.class
            }) {
                reflection.registerType(clazz,
                        INVOKE_DECLARED_CONSTRUCTORS,
                        INVOKE_DECLARED_METHODS,
                        DECLARED_FIELDS);
            }

            // REST model classes (Jackson serialization)
            for (var clazz : new Class<?>[]{
                    AnnotatedSignature.class,
                    BatchRequest.class,
                    BatchResponse.class,
                    FilterOperand.class,
                    HealthResponse.class,
                    PivotDirection.class,
                    SubjectsResponse.class,
                    org.cardanofoundation.tokenmetadata.registry.api.model.rest.TokenMetadata.class,
                    TokenMetadataProperty.class,
                    VerifyFailureResponse.class,
                    WalletFraudIncident.class,
                    WalletHashes.class,
                    WalletTrustCheckResponse.class
            }) {
                reflection.registerType(clazz,
                        INVOKE_DECLARED_CONSTRUCTORS,
                        INVOKE_DECLARED_METHODS,
                        DECLARED_FIELDS);
            }

            // Flyway migration resources
            hints.resources().registerPattern("db/migration/postgresql/*");
            hints.resources().registerPattern("db/store/*");
        }
    }
}
