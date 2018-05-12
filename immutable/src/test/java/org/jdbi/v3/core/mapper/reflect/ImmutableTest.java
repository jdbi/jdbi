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
package org.jdbi.v3.core.mapper.reflect;

import org.jdbi.v3.core.SampleImmutable;
import org.jdbi.v3.core.mapper.MappingException;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ImmutableTest {

    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule();

    RowMapper<SampleImmutable> mapper = ImmutableMapper.of(SampleImmutable.class);

    @Before
    public void setUp() throws Exception {
        dbRule.getSharedHandle()
            .registerRowMapper(ImmutableMapper.factory(SampleImmutable.class));
        dbRule.getSharedHandle().execute("CREATE TABLE immutable_bean (id bigint, name varchar, valueInt integer, inner_id integer)");

        dbRule.getSharedHandle().execute("INSERT INTO immutable_bean VALUES(100, 'Foo', 42, 69)");
    }

    public SampleImmutable execute(String query) {
        return dbRule.getSharedHandle().createQuery(query).mapTo(SampleImmutable.class).findOnly();
    }

    @Test
    public void simpleTest() {
        SampleImmutable sampleImmutable = execute("SELECT id, name, valueInt, inner_id FROM immutable_bean");

        assertThat(sampleImmutable.id()).isEqualTo(100);
        assertThat(sampleImmutable.getName()).isEqualTo("Foo");
        assertThat(sampleImmutable.valueInt()).isEqualTo(42);
        assertThat(sampleImmutable.inner().id()).isEqualTo(69);
        assertThat(sampleImmutable.doSomething(2)).isEqualTo(84);
        assertThat(sampleImmutable.doSomething(4)).isEqualTo(168);
    }

    @Test(expected = MappingException.class)
    public void expectedValueNotSet() throws Exception {
        SampleImmutable sampleImmutable = execute("SELECT id, valueInt, inner_id FROM immutable_bean");

        assertThat(sampleImmutable.id()).isEqualTo(100);
        assertThat(sampleImmutable.getName()).isEqualTo("Foo");
        assertThat(sampleImmutable.valueInt()).isEqualTo(42);
        assertThat(sampleImmutable.inner().id()).isEqualTo(69);
        assertThat(sampleImmutable.doSomething(2)).isEqualTo(84);
        assertThat(sampleImmutable.doSomething(4)).isEqualTo(168);
    }

    @Test
    public void expectedValueNotSetWithDefault() {
        SampleImmutable sampleImmutable = execute("SELECT name, valueInt, inner_id FROM immutable_bean");

        assertThat(sampleImmutable.id()).isEqualTo(0);
        assertThat(sampleImmutable.getName()).isEqualTo("Foo");
        assertThat(sampleImmutable.valueInt()).isEqualTo(42);
        assertThat(sampleImmutable.inner().id()).isEqualTo(69);
        assertThat(sampleImmutable.doSomething(2)).isEqualTo(84);
        assertThat(sampleImmutable.doSomething(4)).isEqualTo(168);
    }

    @Test
    public void shouldHandleColumNameWithUnderscores() {
        SampleImmutable sampleImmutable = execute("SELECT id, name, valueInt AS \"value_int\", inner_id FROM immutable_bean");

        assertThat(sampleImmutable.id()).isEqualTo(100);
        assertThat(sampleImmutable.getName()).isEqualTo("Foo");
        assertThat(sampleImmutable.valueInt()).isEqualTo(42);
        assertThat(sampleImmutable.inner().id()).isEqualTo(69);
        assertThat(sampleImmutable.doSomething(2)).isEqualTo(84);
        assertThat(sampleImmutable.doSomething(4)).isEqualTo(168);
    }

    @Test
    public void shouldBeCaseInSensitiveOfColumnWithUnderscoresAndPropertyNames() {
        SampleImmutable sampleImmutable = execute("SELECT id, name, valueInt AS \"VaLUe_iNt\", inner_id FROM immutable_bean");

        assertThat(sampleImmutable.id()).isEqualTo(100);
        assertThat(sampleImmutable.getName()).isEqualTo("Foo");
        assertThat(sampleImmutable.valueInt()).isEqualTo(42);
        assertThat(sampleImmutable.inner().id()).isEqualTo(69);
        assertThat(sampleImmutable.doSomething(2)).isEqualTo(84);
        assertThat(sampleImmutable.doSomething(4)).isEqualTo(168);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldntHandleEmptyResult() throws Exception {
        execute("SELECT id, name, valueInt AS \"VaLUe_iNt\", inner_id FROM immutable_bean WHERE FALSE");
    }
}
