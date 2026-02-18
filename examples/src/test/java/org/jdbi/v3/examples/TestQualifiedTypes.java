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
package org.jdbi.v3.examples;

import java.util.List;

import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import org.jdbi.core.mapper.reflect.ConstructorMapper;
import org.jdbi.core.mapper.reflect.JdbiConstructor;
import org.jdbi.examples.QualifiedTypes;
import org.jdbi.examples.QualifiedTypes.Colon;
import org.jdbi.examples.QualifiedTypes.Comma;
import org.jdbi.sqlobject.SqlObjectPlugin;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.testing.junit.JdbiExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestQualifiedTypes {

    @RegisterExtension
    public static EmbeddedPgExtension pg = MultiDatabaseBuilder.instanceWithDefaults().build();

    @RegisterExtension
    public JdbiExtension pgExtension = JdbiExtension.postgres(pg)
            .withInitializer((ds, h) -> {
                h.execute("CREATE TABLE data (id serial primary key, comma varchar(50), colon varchar(50))");
                h.execute("INSERT INTO data (comma,colon) VALUES ('one,two,three,four', 'eins:zwei:drei:vier')");
            })
            .withPlugin(new SqlObjectPlugin());

    private DataDao dao;

    @BeforeEach
    public void setUp() {
        var jdbi = pgExtension.getJdbi();
        jdbi.registerRowMapper(Data.class, ConstructorMapper.of(Data.class));
        QualifiedTypes.registerMappers(jdbi);
        this.dao = jdbi.onDemand(DataDao.class);
    }

    @Test
    public void test() {
        var data = dao.getData(1);
        assertThat(data.getComma()).isNotEmpty().containsExactly("one", "two", "three", "four");
        assertThat(data.getColon()).isNotEmpty().containsExactly("eins", "zwei", "drei", "vier");
    }

    public static class Data {
        private final int id;
        private final List<String> comma;
        private final List<String> colon;

        @JdbiConstructor
        public Data(int id, @Comma List<String> comma, @Colon List<String> colon) {
            this.id = id;
            this.comma = comma;
            this.colon = colon;
        }

        public int getId() {
            return id;
        }

        public List<String> getComma() {
            return comma;
        }

        public List<String> getColon() {
            return colon;
        }
    }

    public interface DataDao {
        @SqlQuery("SELECT * FROM data WHERE id = :id")
        Data getData(int id);
    }
}
