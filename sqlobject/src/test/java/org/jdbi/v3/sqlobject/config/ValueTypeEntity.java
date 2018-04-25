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
package org.jdbi.v3.sqlobject.config;

import java.util.Objects;

public class ValueTypeEntity {
    public static final ValueTypeEntity of(StringValue stringValue, LongValue longValue) {
        return new ValueTypeEntity(stringValue, longValue);
    }

    private final StringValue stringValue;
    private final LongValue longValue;

    public ValueTypeEntity(StringValue stringValue, LongValue longValue) {
        this.stringValue = stringValue;
        this.longValue = longValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ValueTypeEntity that = (ValueTypeEntity) o;
        return Objects.equals(stringValue, that.stringValue)
                && Objects.equals(longValue, that.longValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stringValue, longValue);
    }

    @Override
    public String toString() {
        return "ValueTypeEntity{"
                + "stringValue=" + stringValue
                + ", longValue=" + longValue
                + '}';
    }
}
