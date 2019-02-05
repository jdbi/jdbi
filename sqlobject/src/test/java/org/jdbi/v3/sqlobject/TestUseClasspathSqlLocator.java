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

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.core.result.NoResultsException;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.locator.UseClasspathSqlLocator;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestUseClasspathSqlLocator {
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    private Handle handle;

    @Before
    public void setUp() {
        handle = dbRule.getSharedHandle();
        handle.execute("insert into something (id, name) values (6, 'Martin')");
    }

    @Test
    public void testBam() {
        Something s = handle.attach(Cromulence.class).findById(6L);
        assertThat(s.getName()).isEqualTo("Martin");
    }

    @Test
    public void testOverride() {
        Something s = handle.attach(SubCromulence.class).findById(6L);
        assertThat(s.getName()).isEqualTo("overridden");
    }

    @Test
    public void testCachedOverride() {
        Something s = handle.attach(Cromulence.class).findById(6L);
        assertThat(s.getName()).isEqualTo("Martin");

        // and now make sure we don't accidentally cache the statement from above
        s = handle.attach(SubCromulence.class).findById(6L);
        assertThat(s.getName()).isEqualTo("overridden");
    }

    @UseClasspathSqlLocator
    @RegisterRowMapper(SomethingMapper.class)
    public interface Cromulence {
        @SqlQuery
        Something findById(@Bind("id") Long id);
    }

    @RegisterRowMapper(SomethingMapper.class)
    @UseClasspathSqlLocator
    public interface SubCromulence extends Cromulence {}

    @Test
    public void emptyFileYieldsEmptyString() {
        assertThatThrownBy(handle.attach(Blanks.class)::methodWithEmptyFile)
            .describedAs("empty string used as query")
            .isInstanceOf(NoResultsException.class)
            .hasMessageContaining("Statement returned no results")
            .hasMessageContaining("sql=''");
    }

    @Test
    public void missingFileThrows() {
        assertThatThrownBy(handle.attach(Blanks.class)::methodWithoutFile)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cannot find classpath resource")
            .hasMessageContaining("methodWithoutFile.sql");
    }

    @UseClasspathSqlLocator
    public interface Blanks {
        @SqlQuery("empty")
        String methodWithEmptyFile();

        @SqlQuery
        String methodWithoutFile();
    }
}
