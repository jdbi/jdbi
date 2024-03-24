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

import java.util.List;

import oracle.jdbc.OracleTypes;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.statement.Update;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.jdbi.v3.testing.junit5.tc.JdbiTestcontainersExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jdbi.v3.oracle12.OracleReturning.returnParameters;
import static org.jdbi.v3.oracle12.OracleReturning.returningDml;

/**
 * This test uses an oracle instance in a testcontainer.
 */
@Tag("slow")
@Testcontainers
@EnabledOnOs(architectures = {"x86_64", "amd64"})
public class TestOracleReturning {

    static final String CONTAINER_VERSION = "gvenzl/oracle-xe:" + System.getProperty("oracle.container.version", "slim-faststart");

    @Container
    static OracleContainer oc = new OracleContainer(CONTAINER_VERSION);

    @RegisterExtension
    public JdbiExtension oracleExtension = JdbiTestcontainersExtension.instance(oc)
            .withPlugin(new SqlObjectPlugin());

    @BeforeEach
    public void beforeEach() {
        Handle handle = oracleExtension.getSharedHandle();
        handle.execute(
                "create sequence something_id_sequence INCREMENT BY 1 START WITH 100");
        handle.execute(
                "create table something (name varchar(200), id int, constraint something_id primary key (id))");
    }

    @Test
    public void testReturningDmlPositionalParams() {
        Handle h = oracleExtension.getSharedHandle();

        try (Update update = h.createUpdate("insert into something(id, name) values (?, ?) returning id into ?")) {
            List<Integer> ids = update
                    .bind(0, 17)
                    .bind(1, "Brian")
                    .addCustomizer(returnParameters().register(2, OracleTypes.INTEGER))
                    .execute(returningDml())
                    .mapTo(int.class)
                    .list();

            assertThat(ids).containsExactly(17);
        }
    }

    @Test
    public void testReturningDmlNamedParams() {
        Handle h = oracleExtension.getSharedHandle();

        try (Update update = h.createUpdate("insert into something(id, name) values (:id, :name) returning id into :result")) {
            List<Integer> ids = update
                    .bindBean(new Something(20, "Brian"))
                    .addCustomizer(returnParameters().register("result", OracleTypes.INTEGER))
                    .execute(returningDml())
                    .mapTo(int.class)
                    .list();

            assertThat(ids).containsExactly(20);
        }
    }
}
