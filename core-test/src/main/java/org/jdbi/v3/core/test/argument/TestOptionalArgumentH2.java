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
package org.jdbi.v3.core.test.argument;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import org.jdbi.v3.core.test.junit5.H2DatabaseExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class TestOptionalArgumentH2 {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance();

    @BeforeEach
    public void createTable() {
        h2Extension.getJdbi().useHandle(h ->
            h.execute("CREATE TABLE test (id BIGINT PRIMARY KEY, val TEXT)"));
    }

    @Test
    public void testNotOptional() {
        assertThatThrownBy(() -> insert("val.text", new EmptyBean())).isInstanceOf(IllegalArgumentException.class);
        assertThat(select()).isNotPresent();
    }

    @Test
    public void testOptional() {
        insert("val?.text", new EmptyBean());
        Optional<IdValue> op = select();
        assertThat(op).isPresent().get()
                .extracting(x -> x.val).isNull();
        assertThat(op.get().id).isOne();
    }

    @Test
    public void testNotOptionalFullBean() {
        insert("val.text", new FullBean());
        Optional<IdValue> op = select();
        assertThat(op).isPresent().get()
                .extracting(x -> x.val).isEqualTo("TEST");
        assertThat(op.get().id).isOne();
    }

    @Test
    public void testOptionalFullBean() {
        insert("val?.text", new FullBean());
        Optional<IdValue> op = select();
        assertThat(op).isPresent().get()
                .extracting(x -> x.val).isEqualTo("TEST");
        assertThat(op.get().id).isOne();
    }

    private void insert(String binding, Object bean) {
        h2Extension.getJdbi().useHandle(h -> {
            String insert = String.format("INSERT INTO test VALUES(:id, :%s)", binding);
            h.createUpdate(insert).bindBean(bean).execute();
        });
    }

    private Optional<IdValue> select() {
        return h2Extension.getJdbi().withHandle(
            h -> h.createQuery("SELECT id, val FROM test")
                .map((rs, ctx) -> new IdValue(rs.getLong("id"), rs.getString("val")))
                .findFirst());
    }

    public static class FullBean {
        public long getId() {
            return 1;
        }

        public Object getVal() {
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

        public Object getVal() {
            return null;
        }
    }

    private class IdValue {
        private long id;
        private String val;

        IdValue(long id, String val) {
            this.id = id;
            this.val = val;
        }
    }
}
