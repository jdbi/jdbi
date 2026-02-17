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
package org.jdbi.sqlobject;

import org.assertj.core.api.Assertions;
import org.jdbi.core.statement.UnableToExecuteStatementException;
import org.jdbi.sqlobject.statement.SqlUpdate;
import org.jdbi.testing.junit.JdbiExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class TestBridgeException {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withPlugin(new SqlObjectPlugin());

    @BeforeEach
    public void setUp() {
        this.h2Extension.getSharedHandle().execute("CREATE TABLE uniq (id INTEGER PRIMARY KEY)");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void testBridgeExceptionPassthru() {
        final ExceptionalBridge dao = this.h2Extension.getSharedHandle().attach(ExceptionallyTypedBridge.class);
        final Object arg = 3;
        Assertions.assertThatThrownBy(() -> {
            for (int i = 0; i < 2; i++) {
                dao.insert(arg);
            }
        }).isInstanceOf(UnableToExecuteStatementException.class);
    }

    public interface ExceptionalBridge<T> extends SqlObject {
        void insert(T value);
    }

    public interface ExceptionallyTypedBridge extends ExceptionalBridge<Integer> {
        @Override
        @SqlUpdate("INSERT INTO uniq (id) VALUES(:value)")
        void insert(Integer value);
    }
}
