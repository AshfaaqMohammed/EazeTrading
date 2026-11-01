package com.eaze;

import org.springframework.boot.test.context.SpringBootTest;

/**
 * Base class for DB integration tests.
 *
 * NOTE: We intended to use Testcontainers (a throwaway MySQL in Docker), but Docker
 * Hub image pulls were blocked (UNAUTHORIZED) in this environment, so we fall back to
 * a LOCAL MySQL `eaze_test` schema. The locking behaviour under test is identical
 * (real MySQL 8), but this is less portable/CI-friendly than Testcontainers.
 *
 * Datasource + Flyway for tests are configured in src/test/resources/application.properties.
 */
@SpringBootTest
public abstract class AbstractMySqlContainerTest {
    // No container; see test application.properties for the eaze_test datasource.
}