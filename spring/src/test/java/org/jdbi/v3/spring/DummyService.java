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

import org.jdbi.v3.core.Jdbi;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class DummyService implements Service {
    private final Jdbi jdbi;

    public DummyService(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void inPropagationRequired(Callback c) {
        c.call(jdbi);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void inRequiresNew(Callback c) {
        c.call(jdbi);
    }

    @Override
    @Transactional(propagation = Propagation.NESTED)
    public void inNested(Callback c) {
        c.call(jdbi);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_UNCOMMITTED)
    public void inRequiresNewReadUncommitted(Callback c) {
        c.call(jdbi);
    }
}
