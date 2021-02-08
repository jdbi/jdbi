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
package org.jdbi.v3.moshi;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.Set;

import com.squareup.moshi.*;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.json.AbstractJsonMapperTest;
import org.jdbi.v3.json.Json;
import org.jdbi.v3.postgres.PostgresDbRule;
import org.jdbi.v3.testing.JdbiRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestMoshiPlugin extends AbstractJsonMapperTest {
    @Rule
    public JdbiRule db = PostgresDbRule.rule();

    @Before
    public void before() {
        jdbi = db.getJdbi().installPlugin(new MoshiPlugin())
                .configure(MoshiConfig.class, c -> c.setMoshi(
                        new Moshi.Builder().add(new OptionalAdapter()).build()));
    }

    @Test
    public void typeCanBeOverridden() {
        db.getJdbi().useHandle(h -> {
            h.createUpdate("create table users(usr json)").execute();

            Moshi moshi = new Moshi.Builder()
                    .add(new OptionalAdapter())
                    .add(SuperUser.class, new SuperUserAdapter())
                    .add(SubUser.class, new SubUserAdapter())
                    .build();
            h.getConfig(MoshiConfig.class).setMoshi(moshi);

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

    private static class SuperUserAdapter extends JsonAdapter<SuperUser> {
        @Override
        public void toJson(JsonWriter out, SuperUser superUser) throws IOException {
            out.beginObject().name("name").value("super").endObject();
        }

        @Override
        public SuperUser fromJson(JsonReader in) {
            throw new UnsupportedOperationException();
        }
    }

    private static class SubUserAdapter extends JsonAdapter<SubUser> {
        @Override
        public void toJson(JsonWriter out, SubUser subUser) throws IOException {
            out.beginObject().name("name").value("sub").endObject();
        }

        @Override
        public SubUser fromJson(JsonReader in) {
            throw new UnsupportedOperationException();
        }
    }

    private static class OptionalAdapter implements JsonAdapter.Factory {
        @Override
        public JsonAdapter<?> create(Type type, Set<? extends Annotation> annotations, Moshi moshi) {
            Class<?> rawType = Types.getRawType(type);
            if (rawType == Optional.class && type instanceof ParameterizedType) {

                JsonAdapter<Object> delegate = moshi.adapter(((ParameterizedType) type).getActualTypeArguments()[0]);
                return new JsonAdapter<Optional<Object>>() {
                    @Override
                    public Optional<Object> fromJson(JsonReader reader) throws IOException {
                        if (reader.peek() == JsonReader.Token.NULL)
                            return Optional.ofNullable(reader.nextNull());
                        else
                            return Optional.of(delegate.fromJson(reader));
                    }

                    @Override
                    public void toJson(JsonWriter writer, Optional<Object> value) throws IOException {
                        if (value != null && value.isPresent())
                            delegate.toJson(writer, value.get());
                        else
                            writer.nullValue();
                    }
                };
            }

            return null;
        }
    }
}
