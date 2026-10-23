package org.cardanofoundation.tokenmetadata.registry.api.config;

import org.cardanofoundation.tokenmetadata.registry.api.model.cip113.ProgrammableTokenCip113;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip68.FungibleTokenMetadata;
import org.cardanofoundation.tokenmetadata.registry.api.model.v2.Subject;
import org.cardanofoundation.tokenmetadata.registry.entity.Cip113RegistryNode;
import org.cardanofoundation.tokenmetadata.registry.entity.Cip113RegistryNodeId;
import org.cardanofoundation.tokenmetadata.registry.entity.InvalidToken;
import org.cardanofoundation.tokenmetadata.registry.entity.MetadataReferenceNft;
import org.cardanofoundation.tokenmetadata.registry.validation.FieldCategory;
import org.cardanofoundation.tokenmetadata.registry.validation.ValidationError;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The native image keeps only the reflection it is told about: the entities, the types written as JSON
 * (validation errors) and the response types Jackson cannot infer (the CIP-113 extension, held as an interface) must be
 * registered, or the native build fails at runtime when it first reads or writes them.
 */
class NativeImageConfigTest {

    private final RuntimeHints hints = new RuntimeHints();

    NativeImageConfigTest() {
        new NativeImageConfig.Hints().registerHints(hints, getClass().getClassLoader());
    }

    @ParameterizedTest
    @ValueSource(classes = {
            MetadataReferenceNft.class,
            InvalidToken.class,
            Cip113RegistryNode.class,
            Cip113RegistryNodeId.class,
            ValidationError.class,
            FieldCategory.class,
            ProgrammableTokenCip113.class,
            Subject.class,
            FungibleTokenMetadata.class,
            org.cardanofoundation.tokenmetadata.registry.api.model.validate.Cip26ValidateRequest.class,
            org.cardanofoundation.tokenmetadata.registry.api.model.validate.Cip68ValidateRequest.class,
            org.cardanofoundation.tokenmetadata.registry.api.model.validate.Cip68Fields.class,
            org.cardanofoundation.tokenmetadata.registry.api.model.validate.Cip113ValidateRequest.class,
            org.cardanofoundation.tokenmetadata.registry.api.model.validate.ValidateResponse.class,
            org.cardanofoundation.tokenmetadata.registry.api.model.validate.ServedAs.class,
            org.cardanofoundation.tokenmetadata.registry.model.Mapping.class})
    void registersTheTypeForReflection(Class<?> type) {
        assertThat(RuntimeHintsPredicates.reflection().onType(type)).accepts(hints);
    }
}
