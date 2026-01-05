package com.gvn.binarbe;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Disabled in CI - requires actual SQL Server and Redis connections")
class BinarBeApplicationTests {
  @Test
  void contextLoads() {}
}
