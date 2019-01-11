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
package org.jdbi.v3.core.argument;

import java.util.Optional;

import lombok.AllArgsConstructor;
import org.jdbi.v3.core.rule.DatabaseRule;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestOptionalArgumentH2 {
    @Rule
    public DatabaseRule dbRule = new H2DatabaseRule();

    @Before
    public void createTable() {
        dbRule.getJdbi().useHandle(h -> {
            h.execute("CREATE TABLE test (id BIGINT PRIMARY KEY, value TEXT)");
        });
    }

    @Test
    public void testNotOptional() {
        assertThatThrownBy(() -> insert("value.text", new EmptyBean())).isInstanceOf(IllegalArgumentException.class);
        assertThat(select().isPresent()).isFalse();
    }

    @Test
    public void testOptional() {
        insert("value?.text", new EmptyBean());
        Optional<IdValue> op = select();
        assertThat(op.isPresent()).isTrue();
        assertThat(op.get().value).isNull();
        assertThat(op.get().id).isEqualTo(1);
    }

    @Test
    public void testNotOptionalFullBean() {
        insert("value.text", new FullBean());
        Optional<IdValue> op = select();
        assertThat(op.isPresent()).isTrue();
        assertThat(op.get().value).isEqualTo("TEST");
        assertThat(op.get().id).isEqualTo(1);
    }

    @Test
    public void testOptionalFullBean() {
        insert("value?.text", new FullBean());
        Optional<IdValue> op = select();
        assertThat(op.isPresent()).isTrue();
        assertThat(op.get().value).isEqualTo("TEST");
        assertThat(op.get().id).isEqualTo(1);
    }

    private void insert(String binding, Object bean) {
        dbRule.getJdbi().useHandle(h -> {
            String insert = String.format("INSERT INTO test VALUES(:id, :%s)", binding);
            h.createUpdate(insert).bindBean(bean).execute();
        });
    }

    private Optional<IdValue> select() {
        return dbRule.getJdbi().withHandle(
            h -> h.createQuery("SELECT id, value FROM test")
                .map((rs, ctx) -> new IdValue(rs.getLong("id"), rs.getString("value")))
                .findFirst());
    }

    public static class FullBean {
        public long getId() {
            return 1;
        }

        public Object getValue() {
            return new NestedBean();
        }
    }

    public static class NestedBean {
        public String getText() {
            return "TEST";
        }
    }

    public static class EmptyBean {
        public long getId() {
            return 1;
        }

        public Object getValue() {
            return null;
        }
    }

    @AllArgsConstructor
    private class IdValue {
        private long id;
        private String value;
    }
}
