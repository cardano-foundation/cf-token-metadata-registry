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
import org.eclipse.jgit.diff.DiffAlgorithm;
import org.eclipse.jgit.diff.DiffConfig;
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

            // JGit enum-typed git config reflection.
            //
            // org.eclipse.jgit.lib.Config#getEnum reads enum-typed git config values
            // (core.autocrlf, core.eol, core.trustlooserefstat, diff.algorithm,
            // diff.renames, …) by reflectively calling enumClass.getMethod("values").
            // GraalVM's static reachability analysis cannot see those calls, so without
            // explicit hints the synthetic values() method is stripped from the native
            // image and JGit fails at runtime with:
            //
            //   java.lang.IllegalArgumentException: Enumerated values of type
            //     org.eclipse.jgit.<pkg>$<EnumName> not available
            //   Caused by: java.lang.NoSuchMethodException:
            //     org.eclipse.jgit.<pkg>$<EnumName>.values()
            //
            // This is a *class of bug* — every enum-typed git config setting on the
            // JGit call path needs its enum registered for reflection. The enums below
            // cover the CIP-26 offchain sync path (clone → log/diff walk over the
            // cardano-token-registry repo). A future JGit bump can add new enum-typed
            // config options that would surface here as the same error on a different
            // enum class; extend the list as needed.
            for (Class<?> enumClass : new Class<?>[]{
                    // core.* config — read during repo init + working tree setup.
                    CoreConfig.AutoCRLF.class,
                    CoreConfig.CheckStat.class,
                    CoreConfig.EOL.class,
                    CoreConfig.EolStreamType.class,
                    CoreConfig.HideDotFiles.class,
                    CoreConfig.LogRefUpdates.class,
                    CoreConfig.SymLinks.class,
                    CoreConfig.TrustPackedRefsStat.class,
                    CoreConfig.TrustLooseRefStat.class,
                    // diff.* config — read by DiffFormatter when walking commit history
                    // (DiffCommand, LogCommand with path filters, and anything that
                    // invokes DiffFormatter.setRepository / setReader).
                    DiffAlgorithm.SupportedAlgorithm.class,
                    DiffConfig.RenameDetectionType.class
            }) {
                reflection.registerType(enumClass,
                        INVOKE_PUBLIC_METHODS,
                        DECLARED_FIELDS);
            }
        }
    }
}
