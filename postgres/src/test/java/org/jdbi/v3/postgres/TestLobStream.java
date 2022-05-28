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
package org.jdbi.v3.postgres;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.sql.SQLException;
import java.util.function.Supplier;

import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestLobStream {

    private static final int BIG_DATA = 1024 * 1024 * 64;

    private Lobject lob;
    private Handle h;

    @RegisterExtension
    public static EmbeddedPgExtension pg = MultiDatabaseBuilder.instanceWithDefaults()
        .withDatabasePreparer(ds -> Jdbi.create(ds).withHandle(h -> h.execute("CREATE TABLE lob (id int, lob oid)"))).build();

    @RegisterExtension
    public JdbiExtension pgExtension = JdbiExtension.postgres(pg).withPlugins(new SqlObjectPlugin(), new PostgresPlugin());

    @BeforeEach
    public void setUp() throws SQLException {
        this.h = pgExtension.getSharedHandle();
        this.lob = h.attach(Lobject.class);
    }

    @Test
    public void blobCrud() throws IOException {
        final Supplier<InputStream> expectedData = () -> new IntsInputStream(0, 255);
        h.useTransaction(th -> {
            assertThat(lob.countLob()).isZero();
            lob.insert(1, expectedData.get());
            assertThat(lob.countLob()).isOne();
            assertSameBytes(lob.findBlob(1), expectedData.get());
            lob.deleteLob(1);
            assertThat(lob.countLob()).isZero();
            assertThat(lob.findBlob(1)).isNull();
        });
    }

    @Test
    public void clobCrud() throws IOException {
        final Supplier<Reader> expectedData = () -> new IntsReader('0', 'z');
        h.useTransaction(th -> {
            lob.insert(2, expectedData.get());
            assertSameChars(lob.findClob(2), expectedData.get());
            lob.deleteLob(2);
        });
    }

    private void assertSameBytes(InputStream a, InputStream b) throws IOException {
        int pos = 0;
        int read;
        while ((read = a.read()) != -1) {
            assertThat(b.read())
                    .describedAs("byte at position %s", pos)
                    .isEqualTo(read);
            pos++;
        }
        assertThat(b.read()).isEqualTo(-1);
    }

    private void assertSameChars(Reader a, Reader b) throws IOException {
        int pos = 0;
        int achar;
        while ((achar = a.read()) != -1) {
            assertThat(b.read())
                    .describedAs("char at position %s", pos)
                    .isEqualTo(achar);
            pos++;
        }
        assertThat(b.read()).isEqualTo(-1);
    }

    public interface Lobject {
        @SqlUpdate("insert into lob (id, lob) values (:id, :blob)")
        void insert(int id, InputStream blob);
        @SqlUpdate("insert into lob (id, lob) values (:id, :clob)")
        void insert(int id, Reader clob);

        @SqlQuery("select lob from lob where id = :id")
        InputStream findBlob(int id);
        @SqlQuery("select lob from lob where id = :id")
        Reader findClob(int id);

        @SqlUpdate("delete from lob where id = :id returning lo_unlink(lob)")
        void deleteLob(int id);

        @SqlQuery("select count(oid) from pg_largeobject_metadata")
        int countLob();
    }

    class IntsInputStream extends InputStream {
        private final int lo;
        private final int hi;
        private int count;
        private int current;

        IntsInputStream(int lo, int hi) {
            this.lo = lo;
            this.hi = hi;
            current = lo - 1;
        }

        @Override
        public int read() throws IOException {
            if (count++ >= BIG_DATA) {
                return -1;
            }
            if (++current > hi) {
                current = lo;
            }
            return current;
        }
    }

    class IntsReader extends Reader {
        private final char lo;
        private final char hi;
        private int count;
        private char current;

        IntsReader(char lo, char hi) {
            this.lo = lo;
            this.hi = hi;
            current = (char) (lo - 1);
        }

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            int read = 0;
            for (int i = off; i < off + len; i++) {
                if (count++ >= BIG_DATA) {
                    return read > 0 ? read : -1;
                }
                if (++current > hi) {
                    current = lo;
                }
                cbuf[i] = current;
                read++;
            }
            return read;
        }

        @Override
        public void close() throws IOException {}
    }
}
