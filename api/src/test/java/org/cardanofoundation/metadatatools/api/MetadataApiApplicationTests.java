package org.cardanofoundation.metadatatools.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.test.context.ActiveProfiles;

@JdbcTest
@ActiveProfiles("test")
class MetadataApiApplicationTests {
  @Test
  void contextLoads() {}
}
