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
import static org.mockito.Mockito.mock;

import java.util.Calendar;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.Something;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestRegisteredMappers {
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule();
    private Jdbi db;

    @Before
    public void setUp() {
        db = dbRule.getJdbi();
    }

    @Test
    public void testRegisterInferredOnJdbi() {
        db.registerRowMapper(new SomethingMapper());
        Something sam = db.withHandle(handle1 -> {
            handle1.execute("insert into something (id, name) values (18, 'Sam')");

            return handle1.createQuery("select id, name from something where id = :id")
                .bind("id", 18)
                .mapTo(Something.class)
                .findOnly();
        });

        assertThat(sam.getName()).isEqualTo("Sam");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void registerByGenericType() {
        RowMapper<Iterable<Calendar>> mapper = mock(RowMapper.class);
        GenericType<Iterable<Calendar>> iterableOfCalendarType = new GenericType<Iterable<Calendar>>() {};

        db.registerRowMapper(iterableOfCalendarType, mapper);

        assertThat(db.getConfig(RowMappers.class).findFor(iterableOfCalendarType))
            .contains(mapper);
    }
}
