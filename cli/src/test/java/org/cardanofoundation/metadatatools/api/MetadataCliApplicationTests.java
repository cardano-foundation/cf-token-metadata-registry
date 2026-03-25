package org.cardanofoundation.metadatatools.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class MetadataCliApplicationTests {
    @Test
    void contextLoads() {
        assertDoesNotThrow(() -> MetadataCliApplication.main(new String[]{}));
    }
}
