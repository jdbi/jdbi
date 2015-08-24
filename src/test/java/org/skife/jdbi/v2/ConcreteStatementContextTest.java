/*
 * Copyright (C) 2004 - 2014 Brian McCallister
 *
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
package org.skife.jdbi.v2;

import org.junit.Test;

import java.util.Collections;

public class ConcreteStatementContextTest {


    @Test(expected = IllegalArgumentException.class)
    public void testShouldNotBeAbleToCombineGeneratedKeysAndConcurrentUpdatable() throws Exception {
        final ConcreteStatementContext context =
                new ConcreteStatementContext(Collections.<String, Object>emptyMap(), new MappingRegistry());

        context.setReturningGeneratedKeys(true);
        context.setConcurrentUpdatable(true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testShouldNotBeAbleToCombineConcurrentUpdatableAndGeneratedKeys() throws Exception {
        final ConcreteStatementContext context =
                new ConcreteStatementContext(Collections.<String, Object>emptyMap(), new MappingRegistry());

        context.setConcurrentUpdatable(true);
        context.setReturningGeneratedKeys(true);
    }
}
