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

import java.sql.Timestamp;
import java.time.LocalDate;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Check that {@code LocalDate} values are correctly bound both as parameters and result set values.
 */
public class TestBindLocalDate {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withPlugin(new SqlObjectPlugin());

    private Handle handle;

    private Dao dao;

    @BeforeEach
    public void setUp() {
        handle = h2Extension.getSharedHandle();
        dao = handle.attach(Dao.class);
        handle.execute("create table bind_local_date_test ("
            + " id int auto_increment primary key,"
            + " date_column timestamp not null)");
    }

    @AfterEach
    public void tearDown() {
        handle.execute("drop table bind_local_date_test");
    }

    /**
     * Check that inserting a LocalDate value and select it back yields the
     * same date.
     */
    @Test
    public void testBindLocalDate() {
        LocalDate date = LocalDate.of(2001, 2, 1);
        long id = dao.insert(date);

        LocalDate inserted = dao.findById(id);
        assertThat(inserted).isEqualTo(date);
    }

    /**
     * Check that inserting a LocalDate value into a timestamp column and
     * selecting it back yields the inserted date at the start of the day.
     * In other words, verify that clients relying on the old behavior
     * of inserting LocalDate as TIMESTAMP aren't broken by the move
     * to binding LocalDate as DATE.
     */
    @Test
    public void testBindTimestamp() {
        LocalDate date = LocalDate.of(2001, 2, 1);
        long id = dao.insert(date);

        Timestamp inserted = dao.findByIdAsTimestamp(id);
        assertThat(inserted).isEqualTo(Timestamp.valueOf(date.atStartOfDay()));
    }

    public interface Dao {
        @SqlQuery("select date_column from bind_local_date_test where id = :id")
        LocalDate findById(@Bind("id") long id);

        @SqlQuery("select date_column from bind_local_date_test where id = :id")
        Timestamp findByIdAsTimestamp(@Bind("id") long id);

        @GetGeneratedKeys
        @SqlUpdate("insert into bind_local_date_test(date_column) values (:date)")
        long insert(@Bind("date") LocalDate date);
    }
}
