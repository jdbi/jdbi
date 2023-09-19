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
 *
 * @deprecated The <a href="https://github.com/opentable/otj-pg-embedded/">OtjPostgres project</a> ships its artifact as an automatic module with filename
 * based module name. It is strongly discouraged to ship any JPMS enabled project that depends on automatic modules with filename based module names. We
 * have reached out to the OtjPostgres project and <a href="https://github.com/opentable/otj-pg-embedded/pull/184">proposed a change that would fix this</a>.
 * If they ship a version that fixes this before we are ready to move to full JPMS support, we will un-deprecate this class, otherwise it will be removed
 * with our switch to JPMS.
 */
@Deprecated(since = "3.42.0", forRemoval = true) // intellij users need to change the "Usage of API marked for removal" inspection from error to warning
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

        return pg.getJdbcUrl("postgres");
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
    protected void startExtension() throws Exception {

        if (this.epg != null || this.postgresConnection != null) {
            throw new IllegalStateException("Extension was already started!");
        }

        this.epg = this.createEmbeddedPostgres();
        this.postgresConnection = createDataSource().getConnection();

        super.startExtension();
    }

    @Override
    protected void stopExtension() throws Exception {

        if (this.epg == null || this.postgresConnection == null) {
            throw new IllegalStateException("Extension was already stopped!");
        }

        try (EmbeddedPostgres pg = this.epg;
            Connection c = this.postgresConnection) {

            super.stopExtension();
        } finally {
            this.postgresConnection = null;
            this.epg = null;
        }
    }

    private EmbeddedPostgres createEmbeddedPostgres() throws IOException {

        final EmbeddedPostgres.Builder builder = EmbeddedPostgres.builder();
        this.builderCustomizers.forEach(c -> c.accept(builder));

        return builder.start();
    }
}
