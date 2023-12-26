/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jdbi.v3.testing.junit5.tc;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


final class TestcontainersDatabaseInformationSupplier implements Supplier<TestcontainersDatabaseInformation>, AutoCloseable, Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(TestcontainersDatabaseInformationSupplier.class);

    private final TestcontainersDatabaseInformation templateDatabaseInformation;
    private final ExecutorService executor;
    private final SynchronousQueue<TestcontainersDatabaseInformation> nextSchema = new SynchronousQueue<>();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final CountDownLatch stopped = new CountDownLatch(1);
    private volatile DataSource dataSource = null;

    TestcontainersDatabaseInformationSupplier(TestcontainersDatabaseInformation templateDatabaseInformation) {
        this.templateDatabaseInformation = templateDatabaseInformation;

        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread t = new Thread(runnable, "database-schema-creator");
            t.setDaemon(true);
            return t;
        });
    }

    void start(DataSource dataSource) {
        this.dataSource = dataSource;
        this.executor.submit(this);
    }

    @Override
    public void close() {
        LOG.info("Shutdown initiated...");
        if (!this.closed.getAndSet(true)) {
            executor.shutdownNow();
            try {
                if (!stopped.await(10, TimeUnit.SECONDS)) {
                    LOG.warn("Could not shut down database creation thread within 10 seconds");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            LOG.info("Shutdown completed.");
        }
    }

    @Override
    public void run() {
        while (!closed.get()) {
            try {
                final String dbName = EmbeddedUtil.randomLowercase(12);
                final String schemaName = EmbeddedUtil.randomLowercase(12);

                final TestcontainersDatabaseInformation databaseInformation = templateDatabaseInformation.forCatalogAndSchema(
                    templateDatabaseInformation.getCatalog().orElse(dbName),
                    templateDatabaseInformation.getSchema().orElse(schemaName));

                executeStatements(databaseInformation.getCreationScript());

                nextSchema.put(databaseInformation);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break; // while
            } catch (SQLException e) {
                LOG.error("SQL Exception caught:", e);
                break;

            } catch (Exception t) {
                LOG.error("Could not create database:", t);
            }
        }
        stopped.countDown();
    }

    @Override
    public TestcontainersDatabaseInformation get() {

        while (true) {
            if (closed.get()) {
                throw new IllegalStateException("Already closed!");
            }
            try {
                TestcontainersDatabaseInformation schema = nextSchema.poll(100, TimeUnit.MILLISECONDS);
                if (schema != null) {
                    return schema;
                }
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
    }

    private void executeStatements(final List<String> statements) throws SQLException {
        try (Connection c = dataSource.getConnection()) {
            for (String statement : statements) {
                try (Statement stmt = c.createStatement()) {
                    stmt.executeUpdate(statement);
                }
            }
        }
    }
}
