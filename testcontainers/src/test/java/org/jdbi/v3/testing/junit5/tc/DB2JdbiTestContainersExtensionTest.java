package org.jdbi.v3.testing.junit5.tc;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Db2Container;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("slow")
@Testcontainers
public class DB2JdbiTestContainersExtensionTest extends AbstractJdbiTestcontainersExtensionTest {

    @Container
    static JdbcDatabaseContainer<?> dbContainer =
        new Db2Container(DockerImageName.parse("icr.io/db2_community/db2:11.5.9.0"))
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
