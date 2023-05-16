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
package org.jdbi.v3.sqlobject.customizer;

import java.util.Map;
import java.util.UUID;

import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.config.KeyColumn;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MapResultTest {

    @RegisterExtension
    public static EmbeddedPgExtension pg = MultiDatabaseBuilder.instanceWithDefaults().build();

    @RegisterExtension
    JdbiExtension pgExtension = JdbiExtension.postgres(pg)
            .withPlugins(new SqlObjectPlugin());

    Jdbi jdbi;
    UUID content;

    @BeforeEach
    void setUp() {
        this.jdbi = pgExtension.getJdbi();

        jdbi.registerRowMapper(JsonBean.class, ConstructorMapper.of(JsonBean.class));

        jdbi.useHandle(handle -> handle.execute("CREATE TABLE json_data (id INTEGER PRIMARY KEY, key VARCHAR, value UUID)"));

        this.content = UUID.randomUUID();
        jdbi.withHandle(handle -> handle.execute("INSERT INTO json_data (id, key, value) VALUES (?, ?, ?)", 1, "test", content));
    }

    @Test
    public void testMapResult() {
        Map<String, JsonBean> result = jdbi.withExtension(MapDao.class, dao -> dao.getValues(1));

        assertThat(result).isNotNull().hasSize(1);

        JsonBean bean = result.get("test");
        assertThat(bean).isNotNull().extracting("id").isEqualTo(1);
        assertThat(bean).extracting("key").isEqualTo("test");
        assertThat(bean).extracting("value").isEqualTo(content);
    }

    @Test
    public void testNoKeyColumn() {
        assertThatThrownBy(() -> jdbi.withExtension(MapDao.class, dao -> dao.getValuesMissingAnnotation(1)))
                .hasMessageContaining(
                        "Map key column is not declared (missing @KeyColumn annotation?) and no row mapper for key type 'class java.lang.String' is registered!");
    }


    public static final class JsonBean {

        private final int id;
        private final String key;
        private final UUID value;

        public JsonBean(int id, String key, UUID value) {
            this.id = id;
            this.key = key;
            this.value = value;
        }

        public int getId() {
            return id;
        }

        public String getKey() {
            return key;
        }

        public UUID getValue() {
            return value;
        }
    }

    public interface MapDao {

        @SqlQuery("SELECT * FROM json_data WHERE id = :id")
        @KeyColumn("key")
        Map<String, JsonBean> getValues(int id);

        @SqlQuery("SELECT * FROM json_data WHERE id = :id")
        Map<String, JsonBean> getValuesMissingAnnotation(int id);
    }
}
