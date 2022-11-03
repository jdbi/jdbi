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
package org.jdbi.v3.core.statement;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TestSlf4JSqlLogger {
    private static final String CREATE = "create table foo(bar int primary key not null)";
    private static final String INSERT = "insert into foo values (1)";
    private static final String LOGGER_PROPERTY = "org.slf4j.simpleLogger.log.org.jdbi.sql.test";

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance();

    @BeforeEach
    public void before() {
        String oldLevel = null;
        Logger logger;
        try {
            oldLevel = System.getProperty(LOGGER_PROPERTY);
            System.setProperty(LOGGER_PROPERTY, "debug");
            logger = LoggerFactory.getLogger("org.jdbi.sql.test");
        } finally {
            if (oldLevel != null) {
                System.setProperty(LOGGER_PROPERTY, oldLevel);
            }
        }

        h2Extension.getJdbi().getConfig(SqlStatements.class).setSqlLogger(new Slf4JSqlLogger(logger));
    }

    @Test
    void testLogAfterExecutionForBatch() {
        try (Handle handle = h2Extension.openHandle()) {
            handle.execute(CREATE);

            try (Batch batch = handle.createBatch()) {
                batch.add(INSERT);
                assertThatCode(batch::execute).doesNotThrowAnyException();
            }
        }
    }

    @Test
    void testLogExceptionForBatch() {
        try (Handle handle = h2Extension.openHandle();
            Batch batch = handle.createBatch()) {
            batch.add(INSERT);

            assertThatThrownBy(batch::execute)
                    .isInstanceOf(UnableToExecuteStatementException.class);
        }
    }
}
