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
package org.jdbi.v3.core;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Something {
    private int id;
    private String name;
    private Integer integerValue;
    private int intValue;

    public Something(int id, String name) {
        this.id = id;
        this.name = name;
    }

    // Issue #61: @BindBean fails if there is a writable but not readable property, so let's have one...
    public void setWithoutGetter(String bogus) {
        throw new UnsupportedOperationException();
    }
}
