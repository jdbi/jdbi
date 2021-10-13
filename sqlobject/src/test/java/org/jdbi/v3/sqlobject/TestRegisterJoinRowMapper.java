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

import java.util.stream.Stream;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.jdbi.v3.core.mapper.JoinRow;
import org.jdbi.v3.core.mapper.JoinRowMapperTest;
import org.jdbi.v3.core.mapper.JoinRowMapperTest.Article;
import org.jdbi.v3.core.mapper.JoinRowMapperTest.User;
import org.jdbi.v3.sqlobject.config.RegisterJoinRowMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestRegisterJoinRowMapper {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance().withPlugin(new SqlObjectPlugin());

    @BeforeEach
    public void setUp() {
        JoinRowMapperTest t = new JoinRowMapperTest();
        t.h2Extension = h2Extension;
        t.setUp();
    }

    @Test
    public void testSqlObjectJoinRow() {
        Handle handle = h2Extension.getSharedHandle();

        // tag::joinrowusage[]
        Multimap<User, Article> joined = HashMultimap.create();

        handle.attach(UserArticleDao.class)
            .getAuthorship()
            .forEach(jr -> joined.put(jr.get(User.class), jr.get(Article.class)));

        assertThat(joined).isEqualTo(JoinRowMapperTest.getExpected());
        // end::joinrowusage[]
    }

    // tag::joinrowdao[]
    public interface UserArticleDao {

        @RegisterJoinRowMapper({User.class, Article.class})
        @SqlQuery("SELECT * FROM user NATURAL JOIN author NATURAL JOIN article")
        Stream<JoinRow> getAuthorship();
    }
    // end::joinrowdao[]
}
