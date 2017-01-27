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
package org.jdbi.v3.core.array;

import java.util.Optional;

class VendorSupportedArrayType<T> implements SqlArrayType<T> {
    static <T> SqlArrayTypeFactory factory(Class<T> type, String sqlTypeName) {
        SqlArrayType<T> arrayType = new VendorSupportedArrayType<>(sqlTypeName);
        return (t, ctx) -> t.equals(type) ? Optional.of(arrayType) : Optional.empty();
    }

    private final String typeName;

    VendorSupportedArrayType(String typeName) {
        this.typeName = typeName;
    }

    @Override
    public String getTypeName() {
        return typeName;
    }

    @Override
    public Object convertArrayElement(T element) {
        return element;
    }
}
