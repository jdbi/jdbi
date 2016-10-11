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
package org.jdbi.v3.core.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.stream.IntStream;

import org.jdbi.v3.core.H2DatabaseRule;
import org.jdbi.v3.core.Handle;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class JoinRowMapperTest
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();

    private Handle h;

    @Before
    public void setUp()
    {
        h = db.getSharedHandle();
        h.execute("CREATE TABLE user (" +
                    "uid INTEGER NOT NULL," +
                    "name VARCHAR NOT NULL" +
                  ")");
        h.execute("CREATE TABLE article (" +
                    "aid INTEGER NOT NULL," +
                    "title VARCHAR NOT NULL" +
                  ")");
        h.execute("CREATE TABLE author (" +
                    "uid INTEGER NOT NULL," +
                    "aid INTEGER NOT NULL" +
                  ")");

        IntStream.rangeClosed(1, 3).forEach(u ->
            h.execute("INSERT INTO user (uid, name) VALUES (?, ?)", u, "u" + u));

        IntStream.rangeClosed(1, 3).forEach(a ->
            h.execute("INSERT INTO article (aid, title) VALUES (?, ?)", a, "a" + a));

        h.prepareBatch("INSERT INTO author (uid, aid) VALUES (?,?)")
            .add().bind(0, 1).bind(1, 2)
            .next().bind(0, 3).bind(1, 1)
            .next().bind(0, 3).bind(1, 3)
            .submit().execute();

        // tag::mapperSetup[]
        h.registerRowMapper(ConstructorMapper.of(User.class));
        h.registerRowMapper(ConstructorMapper.of(Article.class));
        // end::mapperSetup[]
    }

    @Test
    public void testCartesianProduct() throws Exception
    {
        Multimap<User, Article> product = HashMultimap.create();
        h.createQuery("SELECT * FROM user, article")
            .map(JoinRowMapper.forTypes(User.class, Article.class))
            .forEach(jr -> product.put(jr.get(User.class), jr.get(Article.class)));

        Multimap<User, Article> expected = HashMultimap.create();
        IntStream.rangeClosed(1, 3).forEach(u ->
            IntStream.rangeClosed(1, 3).forEach(a ->
                expected.put(u(u), a(a))));

        assertThat(product).isEqualTo(expected);
    }

    @Test
    public void testJoin() throws Exception
    {
       // tag::multimap[]
        Multimap<User, Article> joined = HashMultimap.create();
        h.createQuery("SELECT * FROM user NATURAL JOIN author NATURAL JOIN article")
            .map(JoinRowMapper.forTypes(User.class, Article.class))
            .forEach(jr -> joined.put(jr.get(User.class), jr.get(Article.class)));
       // end::multimap[]

        assertThat(joined).isEqualTo(getExpected());
    }

    public static Multimap<User, Article> getExpected()
    {
        Multimap<User, Article> expected = HashMultimap.create();
        expected.put(u(1), a(2));
        expected.put(u(3), a(1));
        expected.put(u(3), a(3));
        return expected;
    }

    private static User u(int uid) {
        return new User(uid, "u" + uid);
    }

    private static Article a(int aid) {
        return new Article(aid, "a" + aid);
    }

    public static class User
    {
        private final int uid;
        private final String name;

        public User(int uid, String name) { this.uid = uid; this.name = name; }

        @Override
        public int hashCode() { return Objects.hash(uid, name); }

        @Override
        public boolean equals(Object obj)
        {
            if(obj instanceof User)
            {
                User that = (User) obj;
                return Objects.equals(uid, that.uid) && Objects.equals(name, that.name);
            }
            return false;
        }
    }

    public static class Article
    {
        private final int aid;
        private final String title;

        public Article(int aid, String title) { this.aid = aid; this.title = title; }

        @Override
        public int hashCode() { return Objects.hash(aid, title); }

        @Override
        public boolean equals(Object obj)
        {
            if(obj instanceof Article)
            {
                Article that = (Article) obj;
                return Objects.equals(aid, that.aid) && Objects.equals(title, that.title);
            }
            return false;
        }
    }

}
