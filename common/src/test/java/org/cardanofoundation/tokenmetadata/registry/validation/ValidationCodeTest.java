package org.cardanofoundation.tokenmetadata.registry.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ValidationCode")
class ValidationCodeTest {

    private static List<String> codes() {
        return Arrays.stream(ValidationCode.class.getDeclaredFields())
                .filter(f -> Modifier.isPublic(f.getModifiers()) && Modifier.isStatic(f.getModifiers()) && f.getType() == String.class)
                .filter(f -> !f.getName().equals("DOCUMENTATION"))
                .map(ValidationCodeTest::valueOf)
                .toList();
    }

    private static String valueOf(Field f) {
        try {
            return (String) f.get(null);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    @DisplayName("every code is described in the OpenAPI documentation")
    void everyCodeIsDocumented() {
        assertThat(codes()).isNotEmpty().allSatisfy(code ->
                assertThat(ValidationCode.DOCUMENTATION).as(code).contains("| `" + code + "` |"));
    }

    @Test
    @DisplayName("the documentation describes no code that does not exist")
    void noInventedCodes() {
        long rows = ValidationCode.DOCUMENTATION.lines().filter(l -> l.startsWith("| `")).count();
        assertThat(rows).isEqualTo(codes().size());
    }
}
