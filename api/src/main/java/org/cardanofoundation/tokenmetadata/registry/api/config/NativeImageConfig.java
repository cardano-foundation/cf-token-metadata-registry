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
import org.eclipse.jgit.lib.CoreConfig;
import org.springframework.aot.hint.ReflectionHints;
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
            ReflectionHints reflection = hints.reflection();

            // JPA entities
            for (Class<?> clazz : new Class<?>[]{
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
            for (Class<?> clazz : new Class<?>[]{
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

            // Flyway migration resources — register directories and files explicitly
            // so Flyway's ClassPathScanner can enumerate them in native images.
            // Application migrations
            hints.resources().registerPattern("db/migration/postgresql/.*");
            // Yaci Store migrations (from dependency JARs)
            hints.resources().registerPattern("db/store/postgresql/.*");
            hints.resources().registerPattern("db/store/mysql/.*");
            hints.resources().registerPattern("db/store/h2/.*");

            // JGit CoreConfig nested enums.
            //
            // org.eclipse.jgit.lib.Config#getEnum reads enum-typed git config values
            // (core.autocrlf, core.eol, core.trustlooserefstat, …) by reflectively
            // calling enumClass.getMethod("values"). GraalVM's static reachability
            // analysis cannot see those calls, so without explicit hints the synthetic
            // values() method is stripped from the native image and JGit fails on
            // CloneCommand with:
            //
            //   java.lang.IllegalArgumentException: Enumerated values of type
            //     org.eclipse.jgit.lib.CoreConfig$<EnumName> not available
            //   Caused by: java.lang.NoSuchMethodException:
            //     org.eclipse.jgit.lib.CoreConfig$<EnumName>.values()
            //
            // Observed in production on JGit 7.1.0 with TrustLooseRefStat — the rest
            // are registered preemptively because the same code path
            // (Config.allValuesOf) covers every CoreConfig enum and a future JGit
            // bump can add new ones quietly.
            for (Class<?> enumClass : new Class<?>[]{
                    CoreConfig.AutoCRLF.class,
                    CoreConfig.CheckStat.class,
                    CoreConfig.EOL.class,
                    CoreConfig.EolStreamType.class,
                    CoreConfig.HideDotFiles.class,
                    CoreConfig.LogRefUpdates.class,
                    CoreConfig.SymLinks.class,
                    CoreConfig.TrustPackedRefsStat.class,
                    CoreConfig.TrustLooseRefStat.class
            }) {
                reflection.registerType(enumClass,
                        INVOKE_PUBLIC_METHODS,
                        DECLARED_FIELDS);
            }
        }
    }
}
