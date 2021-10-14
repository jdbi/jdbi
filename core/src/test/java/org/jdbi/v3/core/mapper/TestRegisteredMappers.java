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

import java.util.Calendar;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.junit5.DatabaseExtension;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class TestRegisteredMappers {

    @RegisterExtension
    public DatabaseExtension h2Extension = H2DatabaseExtension.withSomething();

    @Test
    public void testRegisterInferredOnJdbi() {
        Jdbi db = h2Extension.getJdbi();

        db.registerRowMapper(new SomethingMapper());
        Something sam = db.withHandle(handle1 -> {
            handle1.execute("insert into something (id, name) values (18, 'Sam')");

            return handle1.createQuery("select id, name from something where id = :id")
                .bind("id", 18)
                .mapTo(Something.class)
                .one();
        });

        assertThat(sam.getName()).isEqualTo("Sam");
    }

    @Test
    public void registerByGenericType() {
        Jdbi db = h2Extension.getJdbi();

        @SuppressWarnings("unchecked")
        RowMapper<Iterable<Calendar>> mapper = mock(RowMapper.class);
        GenericType<Iterable<Calendar>> iterableOfCalendarType = new GenericType<Iterable<Calendar>>() {};

        db.registerRowMapper(iterableOfCalendarType, mapper);

        assertThat(db.getConfig(RowMappers.class).findFor(iterableOfCalendarType))
            .contains(mapper);
    }
}
