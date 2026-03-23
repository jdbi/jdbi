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
package org.jdbi.v3.oracle12;

import org.jdbi.v3.core.AbstractJavaTimeTests;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.jdbi.v3.testing.junit5.tc.JdbiTestcontainersExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.oracle.OracleContainer;

@Tag("slow")
@Testcontainers
public class TestJavaTime extends AbstractJavaTimeTests {

    static final String CONTAINER_VERSION = "gvenzl/oracle-free:" + System.getProperty("oracle.container.version", "slim-faststart");

    @Container
    static OracleContainer oc = new OracleContainer(CONTAINER_VERSION);

    @RegisterExtension
    JdbiExtension oracleExtension = JdbiTestcontainersExtension.instance(oc)
        .withPlugins(new OraclePlugin());

    @BeforeEach
    public void setUp() {
        h = oracleExtension.openHandle();
        h.useTransaction(th -> {
            th.execute("drop table if exists stuff");
            th.execute("create table stuff (ts timestamp, d date, z varchar2(64), tstz timestamp with time zone)");
        });
    }

    @AfterEach
    public void tearDown() {
        h.close();
    }

    /**
     * Oracle does not support TIME data type.
     */
    @Override
    protected boolean skipLocalTime() {
        return true;
    }

    /**
     * Oracle does not support OffsetDateTime/ZonedDateTime on a TIMESTAMP column.
     */
    @Override
    protected boolean skipObjectTimestamp() {
        return true;
    }
}
