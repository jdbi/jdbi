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
package org.jdbi.gson2;

import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import net.dongliu.gson.GsonJava8TypeAdapterFactory;
import org.jdbi.core.qualifier.QualifiedType;
import org.jdbi.json.AbstractJsonMapperTest;
import org.jdbi.json.Json;
import org.jdbi.postgres.PostgresPlugin;
import org.jdbi.sqlobject.SqlObjectPlugin;
import org.jdbi.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestGson2Plugin extends AbstractJsonMapperTest {

    @RegisterExtension
    public static EmbeddedPgExtension pg = MultiDatabaseBuilder.instanceWithDefaults().build();

    @RegisterExtension
    JdbiExtension pgExtension = JdbiExtension.postgres(pg)
        .withPlugins(new SqlObjectPlugin(), new PostgresPlugin(), new Gson2Plugin())
        .withConfig(Gson2Config.class, g -> g.setGson(
            new GsonBuilder()
                .registerTypeAdapterFactory(new GsonJava8TypeAdapterFactory())
                .create()));

    @BeforeEach
    public void before() {
        jdbi = pgExtension.getJdbi();
    }

    @Test
    public void typeCanBeOverridden() {
        pgExtension.getJdbi().useHandle(h -> {
            h.createUpdate("create table users(usr json)").execute();

            Gson gson = new GsonBuilder()
                .registerTypeAdapter(SuperUser.class, new SuperUserAdapter())
                .registerTypeAdapter(SubUser.class, new SubUserAdapter())
                .create();
            h.getConfig(Gson2Config.class).setGson(gson);

            h.createUpdate("insert into users(usr) values(:user)")
                // declare that the subuser should be mapped as a superuser
                .bindByType("user", new SubUser(), QualifiedType.of(SuperUser.class).with(Json.class))
                .execute();

            User subuser = h.createQuery("select usr from users")
                .mapTo(QualifiedType.of(User.class).with(Json.class))
                .one();

            assertThat(subuser.name)
                .describedAs("instead of being bound via getClass(), the object was bound according to the type param")
                .isEqualTo("super");
        });
    }

    public static class User {
        private final String name;

        public User(String name) {
            this.name = name;
        }
    }

    private static class SuperUser {}

    private static class SubUser extends SuperUser {}

    private static class SuperUserAdapter extends TypeAdapter<SuperUser> {
        @Override
        public void write(JsonWriter out, SuperUser user) throws IOException {
            out.beginObject().name("name").value("super").endObject();
        }

        @Override
        public SuperUser read(JsonReader in) {
            throw new UnsupportedOperationException();
        }
    }

    private static class SubUserAdapter extends TypeAdapter<SubUser> {
        @Override
        public void write(JsonWriter out, SubUser user) throws IOException {
            out.beginObject().name("name").value("sub").endObject();
        }

        @Override
        public SubUser read(JsonReader in) {
            throw new UnsupportedOperationException();
        }
    }
}
