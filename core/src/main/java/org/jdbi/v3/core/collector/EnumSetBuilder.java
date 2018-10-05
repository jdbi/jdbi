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
package org.jdbi.v3.core.collector;

import java.util.EnumSet;

class EnumSetBuilder {

    private final EnumSet set;

    @SuppressWarnings("unchecked")
    EnumSetBuilder(Class<? extends Enum> componentType) {
        this.set = EnumSet.noneOf(componentType);
    }

    @SuppressWarnings("unchecked")
    public void add(Object element) {
        // EnumSet.add() checks against componentType and throws ClassCastException in case of mismatch
        set.add(element);
    }

    @SuppressWarnings("unchecked")
    public static EnumSetBuilder combine(EnumSetBuilder a, EnumSetBuilder b) {
        if (b.set.size() < a.set.size()) {
            a.set.addAll(b.set);
            return a;
        } else {
            b.set.addAll(a.set);
            return b;
        }
    }

    public EnumSet build() {
        return set;
    }
}
