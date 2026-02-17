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
package org.jdbi.core.mapper;

import java.util.Map;

import org.jdbi.core.Handle;
import org.jdbi.core.junit5.SqliteDatabaseExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestMapMapper {

    // h2 makes column names uppercase
    @RegisterExtension
    public SqliteDatabaseExtension sqliteExtension = SqliteDatabaseExtension.instance();

    private Handle h;

    @BeforeEach
    public void before() {
        h = sqliteExtension.getSharedHandle();

        h.execute("create table Foo (Id int primary key, FirstName varchar)");
        h.execute("insert into Foo(Id, FirstName) values(1, 'No one')");
    }

    @Test
    public void testCaseDefaultNop() {
        h.getConfig(MapMappers.class).setCaseChange(CaseStrategy.NOP);

        Map<String, Object> noOne = h.createQuery("select * from Foo").mapToMap().one();

        assertThat(noOne).containsOnlyKeys("Id", "FirstName");
    }

    @Test
    public void testCaseLower() {
        h.getConfig(MapMappers.class).setCaseChange(CaseStrategy.LOCALE_LOWER);

        Map<String, Object> noOne = h.createQuery("select * from Foo").mapToMap().one();

        assertThat(noOne).containsOnlyKeys("id", "firstname");
    }

    @Test
    public void testCaseUpper() {
        h.getConfig(MapMappers.class).setCaseChange(CaseStrategy.LOCALE_UPPER);

        Map<String, Object> noOne = h.createQuery("select * from Foo").mapToMap().one();

        assertThat(noOne).containsOnlyKeys("ID", "FIRSTNAME");
    }
}
