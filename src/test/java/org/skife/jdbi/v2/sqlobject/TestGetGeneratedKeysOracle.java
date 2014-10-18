/*
 * Copyright (C) 2004 - 2014 Brian McCallister
 *
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
package org.skife.jdbi.v2.sqlobject;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.sqlobject.mixins.CloseMe;
import org.skife.jdbi.v2.tweak.HandleCallback;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * This test assumes an instance of Oracle database called 'test' is
 * running on localhost port 1521.
 *
 * @author Ricco Førgaard mailto:ricco@vimond.com
 * @since 2014-10-18
 */
public class TestGetGeneratedKeysOracle {

    private DBI dbi;

    @Before
    public void setUp() throws Exception {
        dbi = new DBI("jdbc:oracle:thin:@localhost:test", "oracle", "oracle");
        dbi.withHandle(new HandleCallback<Object>() {
            public Object withHandle(Handle handle) throws Exception
            {
                handle.execute("create sequence something_id_sequence INCREMENT BY 1 START WITH 100");
                handle.execute("create table something (name varchar(200), id int, constraint something_id primary key (id))");
                return null;
            }
        });
    }

    @After
    public void tearDown() throws Exception {
        dbi.withHandle(new HandleCallback<Object>() {
            public Object withHandle(Handle handle) throws Exception
            {
                handle.execute("drop table something");
                handle.execute("drop sequence something_id_sequence");
                return null;
            }
        });
    }

    /**
     * Oracle needs to be queried by index and not id (like
     * {@link org.skife.jdbi.v2.sqlobject.FigureItOutResultSetMapper} does).
     */
    public static class OracleGeneratedKeyMapper implements ResultSetMapper<Long> {

        @Override
        public Long map(int index, ResultSet r, StatementContext ctx) throws SQLException {
            return r.getLong(1);
        }
    }

    public static interface DAO extends CloseMe {
        @SqlUpdate("insert into something (name, id) values (:name, something_id_sequence.nextval)")
        @GetGeneratedKeys(columnName = "id", value = OracleGeneratedKeyMapper.class)
        public long insert(@Bind("name") String name);

        @SqlQuery("select name from something where id = :it")
        public String findNameById(@Bind long id);
    }

    @Ignore
    @Test
    public void testGetGeneratedKeys() throws Exception {
        DAO dao = dbi.open(DAO.class);

        Long fooId = dao.insert("Foo");
        long barId = dao.insert("Bar");

        assertThat(dao.findNameById(fooId), equalTo("Foo"));
        assertThat(dao.findNameById(barId), equalTo("Bar"));
    }
}
