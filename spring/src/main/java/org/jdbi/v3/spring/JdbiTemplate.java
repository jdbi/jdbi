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
package org.jdbi.v3.spring;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.HandleCallback;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.meta.Beta;

/**
 * Spring template for querying with Jdbi. This class is transaction aware and respects spring's
 * {@link org.springframework.transaction.annotation.Transactional}
 */
@Beta
public class JdbiTemplate implements JdbiOperations {

    private final Jdbi jdbi;

    @Beta
    public JdbiTemplate(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    /**
     * like {@link Jdbi#withHandle(HandleCallback)} but respects springs transactions
     */
    @Override
    public <R, X extends Exception> R withHandle(HandleCallback<R, X> callback) throws X {
        Handle h = JdbiUtil.getHandle(jdbi);
        try {
            return callback.withHandle(h);
        } finally {
            JdbiUtil.closeIfNeeded(h);
        }
    }

    /**
     * @return the underlying Jdbi instance
     */
    public Jdbi getJdbi() {
        return jdbi;
    }
}
