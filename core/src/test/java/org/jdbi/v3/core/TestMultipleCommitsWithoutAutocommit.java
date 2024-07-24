package org.jdbi.v3.core;

import org.assertj.core.api.Assertions;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.sql.Connection;
import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;

public class TestMultipleCommitsWithoutAutocommit {
    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance();
    @BeforeEach
    public void startUp() {
        Assertions.setMaxStackTraceElementsDisplayed(100);
    }

    @Test
    public void testMultipleCommitsWithoutAutocommit() throws Exception {
        Jdbi jdbi = Jdbi.create(() -> {
            // create connection with auto-commit == false
            Connection connection = DriverManager.getConnection(h2Extension.getUri());
            connection.setAutoCommit(false);
            return connection;
        });
        try (Handle handle = jdbi.open()) {
            handle.execute("create table names(name varchar)");
            handle.commit();
            handle.execute("insert into names (name) values ('Kafka')");
            handle.commit();
        }
        try (Handle handle = jdbi.open()) {
            assertThat(handle.createQuery("select count(1) from names").mapTo(Integer.class).one())
                .isEqualTo(1);
        }
    }
}
