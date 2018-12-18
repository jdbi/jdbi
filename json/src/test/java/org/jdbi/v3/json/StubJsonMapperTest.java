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
package org.jdbi.v3.json;

import org.jdbi.v3.core.internal.AnnotationFactory;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.result.ResultIterable;
import org.jdbi.v3.core.result.UnableToProduceResultException;
import org.jdbi.v3.core.statement.UnableToCreateStatementException;
import org.jdbi.v3.core.statement.Update;
import org.jdbi.v3.testing.JdbiRule;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class StubJsonMapperTest {
    @Rule
    public final JdbiRule h2 = JdbiRule.h2().withPlugins();

    @Test
    public void stubIsInstalled() {
        h2.getJdbi().useHandle(h -> {
            h.createUpdate("create table json(val varchar)").execute();

            Update insert = h.createUpdate("insert into json(val) values(:json)")
                .bindByType("json", new Object(), QualifiedType.of(Object.class, AnnotationFactory.create(Json.class)));

            assertThatThrownBy(insert::execute)
                .isInstanceOf(UnableToCreateStatementException.class)
                .hasMessageContaining("need to install a JsonMapper");

            ResultIterable<?> select = h.createQuery("select '{}'")
                .mapTo(QualifiedType.of(String.class, AnnotationFactory.create(Json.class)));

            assertThatThrownBy(select::findOnly)
                .isInstanceOf(UnableToProduceResultException.class)
                .hasMessageContaining("need to install a JsonMapper");
        });
    }
}
