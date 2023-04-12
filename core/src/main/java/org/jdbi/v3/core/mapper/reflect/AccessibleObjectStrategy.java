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

import java.lang.reflect.AccessibleObject;
import java.util.function.Consumer;

import org.jdbi.v3.meta.Alpha;

@Alpha
public enum AccessibleObjectStrategy implements Consumer<AccessibleObject> {
    /**
     * Force non-public methods and constructors to be accessible. Doing this
     * has been discouraged since Java 9 and will be removed in a future Java
     * version. While this is the current default behavior of Jdbi, it will
     * change in the near future due to these changes in the Java platform.
     */
    FORCE_MAKE_ACCESSIBLE {
        @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
        @Override
        public void accept(AccessibleObject accessibleObject) {
            accessibleObject.setAccessible(true);
        }
    },

    /**
     * Do not make non-public methods and constructors accessible.
     */
    DO_NOT_MAKE_ACCESSIBLE {
        @Override
        public void accept(AccessibleObject accessibleObject) {}
    }
}
