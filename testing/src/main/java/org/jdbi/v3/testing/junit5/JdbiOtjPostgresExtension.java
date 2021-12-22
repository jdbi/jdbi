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
package org.jdbi.v3.testing.junit5;

import java.io.IOException;
import java.sql.Connection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import javax.sql.DataSource;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Jdbi PostgreSQL JUnit 5 rule using the otj-pg-embedded component.
 *
 * Override methods for special case construction:
 *
 * <pre>{@code
 *     @RegisterExtension
 *     public JdbiExtension extension = new JdbiOtjPostgresExtension() {
 *         @Override
 *         protected DataSource createDataSource() {
 *            ...
 *         }
 *     };
 * }</pre>
 *
 * Use with {@link org.junit.jupiter.api.extension.ExtendWith}:
 *
 * <pre>{@code
 * @ExtendWith(JdbiOtjPostgresExtension.class)
 * public class DatabaseTest {
 *     @Test
 *     public void testWithJdbi(Jdbi jdbi) {
 *         ...
 *     }
 *
 *     @Test
 *     public void testWithHandle(Handle handle) {
 *         ...
 *     }
 * }
 * }</pre>
 */
public class JdbiOtjPostgresExtension extends JdbiExtension {

    private volatile EmbeddedPostgres epg;
    private volatile Connection postgresConnection;
    private final List<Consumer<EmbeddedPostgres.Builder>> builderCustomizers = new CopyOnWriteArrayList<>();

    static JdbiOtjPostgresExtension instance() {
        return new JdbiOtjPostgresExtension();
    }

    public JdbiOtjPostgresExtension() {}

    @Override
    public String getUrl() {
        final EmbeddedPostgres pg = this.epg;
        if (pg == null) {
            throw new IllegalStateException("not within a Junit test!");
        }

        return pg.getJdbcUrl("postgres", "postgres");
    }

    @Override
    protected DataSource createDataSource() throws Exception {
        final EmbeddedPostgres pg = this.epg;
        if (pg == null) {
            throw new IllegalStateException("not within a Junit test!");
        }

        return pg.getPostgresDatabase();
    }

    public JdbiOtjPostgresExtension customize(Consumer<EmbeddedPostgres.Builder> customizer) {
        this.builderCustomizers.add(customizer);

        return this;
    }

    @Override
    @SuppressWarnings("PMD.UseTryWithResources")
    public void afterEach(ExtensionContext context) throws Exception {

        super.afterEach(context);

        final EmbeddedPostgres pg = this.epg;
        final Connection c = this.postgresConnection;

        if (pg == null || c == null) {
            throw new IllegalStateException("not within a Junit test!");
        }

        try {
            this.postgresConnection = null;
            c.close();
        } finally {
            this.epg = null;
            pg.close();
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        this.epg = this.createEmbeddedPostgres();
        this.postgresConnection = createDataSource().getConnection();

        super.beforeEach(context);
    }

    private EmbeddedPostgres createEmbeddedPostgres() throws IOException {

        final EmbeddedPostgres.Builder builder = EmbeddedPostgres.builder();
        this.builderCustomizers.forEach((c) -> {
            c.accept(builder);
        });

        return builder.start();
    }
}
