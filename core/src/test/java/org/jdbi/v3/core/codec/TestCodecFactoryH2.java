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
package org.jdbi.v3.core.codec;

import static org.junit.Assert.assertEquals;

import java.util.Objects;
import java.util.function.Function;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.rule.DatabaseRule;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.junit.Rule;
import org.junit.Test;

public class TestCodecFactoryH2 {

    @Rule
    public DatabaseRule dbRule = new H2DatabaseRule();

    @Test
    public void testCodecFactory() throws Exception {
        QualifiedType<TestValue> testType = QualifiedType.of(TestValue.class);

        Jdbi jdbi = dbRule.getJdbi();

        jdbi.registerCodecFactory(CodecFactory.forSingleCodec(testType, new TestValueCodec()));

        jdbi.useHandle(h -> {
            h.execute("CREATE TABLE test (test VARCHAR PRIMARY KEY)");
        });

        TestValue value = new TestValue(12345);

        int result = jdbi.withHandle(h -> h.createUpdate("INSERT INTO test (test) VALUES (:test)")
            .bindByType("test", value, testType)
            .execute());

        assertEquals(1, result);

        TestValue response = jdbi.withHandle(h -> h.createQuery("SELECT * from test")
            .mapTo(testType).first());

        assertEquals(value, response);
    }

    public static class TestValueCodec implements Codec<TestValue> {

        @Override
        public ColumnMapper<TestValue> getColumnMapper() {
            return (r, idx, ctx) -> TestValue.create(r.getString(idx));
        }

        @Override
        public Function<TestValue, Argument> getArgumentFunction() {
            return value -> (pos, stmt, ctx) -> stmt.setString(pos, value.getValue());
        }
    }

    public static class TestValue {

        private final long value;

        public static TestValue create(String value) {
            return new TestValue(Long.parseLong(value, 8));
        }

        public TestValue(long value) {
            this.value = value;
        }

        public String getValue() {
            return Long.toString(value, 8);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            TestValue testValue = (TestValue) o;
            return value == testValue.value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }
}
