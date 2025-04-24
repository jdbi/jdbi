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
package org.jdbi.v3.core.statement;

import java.util.Collection;
import java.util.Optional;

import org.jdbi.v3.core.argument.Argument;

/**
 * Provide test access to binding methods that were declared non-public in 4.0.0.
 */
public final class BindingAccess {

    private BindingAccess() {
        throw new AssertionError("BindingAccess can not be instantiated");
    }

    public static Optional<Argument> findForName(Binding binding, String name) {
        return binding.findForName(name);
    }

    public static Collection<String> getNames(Binding binding) {
        return binding.getNames();
    }
}
