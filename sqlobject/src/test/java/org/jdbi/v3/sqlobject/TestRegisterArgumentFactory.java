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
package org.jdbi.v3.sqlobject;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.lang.reflect.Type;
import java.util.Optional;

import org.jdbi.v3.Jdbi;
import org.jdbi.v3.H2DatabaseRule;
import org.jdbi.v3.StatementContext;
import org.jdbi.v3.sqlobject.customizers.RegisterArgumentFactory;
import org.jdbi.v3.tweak.Argument;
import org.jdbi.v3.tweak.ArgumentFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestRegisterArgumentFactory
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    @Before
    public void setUp() throws Exception
    {
        Jdbi dbi = db.getJdbi();
    }

    @Test
    public void testFoo() throws Exception
    {
        db.getJdbi().useExtension(Waffle.class, w -> {
            w.insert(1, new Name("Brian", "McCallister"));

            assertThat(w.findName(1), equalTo("Brian McCallister"));
        });
    }


    @RegisterArgumentFactory(NameAF.class)
    public interface Waffle
    {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insert(@Bind("id") int id, @Bind("name") Name name);

        @SqlQuery("select name from something where id = :id")
        String findName(@Bind("id") int id);
    }

    public static class NameAF implements ArgumentFactory
    {
        @Override
        public Optional<Argument> build(Type expectedType, Object value, StatementContext ctx) {
            if (expectedType == Name.class || value instanceof Name) {
                Name nameValue = (Name) value;
                return Optional.of((position, statement, ctx1) -> statement.setString(position, nameValue.getFullName()));
            }
            return Optional.empty();
        }
    }

    public static class Name
    {
        private final String first;
        private final String last;

        public Name(String first, String last)
        {

            this.first = first;
            this.last = last;
        }

        public String getFullName()
        {
            return first + " " + last;
        }

        @Override
        public String toString()
        {
            return "<Name first=" + first + " last=" + last + " >";
        }
    }

}
