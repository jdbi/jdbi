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

import java.sql.Types;

import org.jdbi.v3.core.statement.Call;
import org.jdbi.v3.core.statement.OutParameters;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.jdbi.v3.testing.junit5.tc.JdbiTestcontainersExtension;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.oracle.OracleContainer;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("slow")
@Testcontainers
public class TestOracleInOutParameter {

    static final String CONTAINER_VERSION = "gvenzl/oracle-free:" + System.getProperty("oracle.container.version", "slim-faststart");

    @Container
    static OracleContainer oc = new OracleContainer(CONTAINER_VERSION);

    @RegisterExtension
    JdbiExtension oracleExtension = JdbiTestcontainersExtension.instance(oc)
            .withInitializer((ds, h) -> {
                h.execute("""
                        CREATE OR REPLACE PROCEDURE double_value(v IN OUT NUMBER) AS
                        BEGIN
                        v := v * 2;
                        END;""");
                h.execute("""
                        CREATE OR REPLACE PROCEDURE concat_value(prefix IN VARCHAR2, v IN OUT VARCHAR2) AS
                        BEGIN
                        v := prefix || v;
                        END;""");
            });

    @Test
    public void testNamedInOutParameter() {
        try (Call call = oracleExtension.getSharedHandle().createCall("call double_value(:v)")) {
            OutParameters out = call
                    .bind("v", 21)
                    .registerOutParameter("v", Types.INTEGER)
                    .invoke();

            assertThat(out.getInt("v")).isEqualTo(42);
        }
    }

    @Test
    public void testPositionalInOutParameter() {
        try (Call call = oracleExtension.getSharedHandle().createCall("call double_value(?)")) {
            OutParameters out = call
                    .bind(0, 21)
                    .registerOutParameter(0, Types.INTEGER)
                    .invoke();

            assertThat(out.getInt(0)).isEqualTo(42);
        }
    }

    @Test
    public void testMixedInAndInOutParameters() {
        try (Call call = oracleExtension.getSharedHandle().createCall("call concat_value(:prefix, :v)")) {
            OutParameters out = call
                    .bind("prefix", "Hello, ")
                    .bind("v", "World")
                    .registerOutParameter("v", Types.VARCHAR)
                    .invoke();

            assertThat(out.getString("v")).isEqualTo("Hello, World");
        }
    }

    @Test
    public void testNullInOutParameter() {
        try (Call call = oracleExtension.getSharedHandle().createCall("call double_value(:v)")) {
            OutParameters out = call
                    .bindByType("v", null, Integer.class)
                    .registerOutParameter("v", Types.INTEGER)
                    .invoke();

            assertThat((Object) out.getInt("v")).isNull();
        }
    }
}
