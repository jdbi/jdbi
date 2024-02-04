package org.jdbi.v3.testing.junit5.tc;

import org.junit.jupiter.api.Tag;
import org.testcontainers.containers.Db2Container;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

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
}
