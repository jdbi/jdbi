package org.jdbi.v3.oracle12;

import java.time.Duration;

import org.testcontainers.containers.OracleContainer;

public final class JdbiOracleContainer {
    private static final String CONTAINER_VERSION =
            "gvenzl/oracle-xe:" + System.getProperty("oracle.container.version", "slim-faststart");

    private JdbiOracleContainer() {}

    @SuppressWarnings("resource")
    public static OracleContainer create() {
        return new OracleContainer(CONTAINER_VERSION)
                .withStartupTimeout(Duration.ofMinutes(10));
    }
}
