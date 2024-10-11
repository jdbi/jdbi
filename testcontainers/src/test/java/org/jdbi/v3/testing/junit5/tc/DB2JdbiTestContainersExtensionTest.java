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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.testcontainers.containers.Db2Container;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("slow")
@Testcontainers
@EnabledOnOs(architectures = { "x86_64", "amd64" })
public class DB2JdbiTestContainersExtensionTest extends AbstractJdbiTestcontainersExtensionTest {

    @Container
    static JdbcDatabaseContainer<?> dbContainer =
        new Db2Container(DockerImageName.parse("icr.io/db2_community/db2:11.5.9.0")
            .asCompatibleSubstituteFor("ibmcom/db2"))
            .acceptLicense();

    @Override
    JdbcDatabaseContainer<?> getDbContainer() {
        return dbContainer;
    }

    @Test
    public void testByteaArrayMultiRows() {
        assertThat(getUserNameAsBlobById(1)).isEqualTo("Alice".getBytes());
        assertThat(getUserNameAsBlobById(2)).isEqualTo("Bob".getBytes());
    }

    private byte[] getUserNameAsBlobById(Integer id) {
        var result = extension.getJdbi().withHandle(handle ->
            handle.createQuery("SELECT BLOB(name) FROM users WHERE id = :id")
                .bind("id", id)
                .mapTo(byte[].class)
                .first()
        );
        assertThat(result).isInstanceOf(byte[].class);
        return result;
    }
}
