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
package org.jdbi.vavr;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TupleMappersTest {

    @Test
    void columnWithersDoNotMutateReceiver() {
        TupleMappers base = new TupleMappers();

        TupleMappers derived = base.keyColumn("k").valueColumn("v").column(3, "third");

        assertThat(base.getKeyColumn()).isNull();
        assertThat(base.getValueColumn()).isNull();
        assertThat(base.getColumn(3)).isNull();

        assertThat(derived.getKeyColumn()).isEqualTo("k");
        assertThat(derived.getValueColumn()).isEqualTo("v");
        assertThat(derived.getColumn(3)).isEqualTo("third");
        assertThat(derived).isNotSameAs(base);
    }

    @Test
    void gettersReturnTupleSpecificColumnsOnlyWithoutGlobalFallback() {
        // TupleMappers no longer falls back to MapEntryMappers in its getters; the reader does that.
        TupleMappers base = new TupleMappers();
        assertThat(base.getKeyColumn()).isNull();
        assertThat(base.getValueColumn()).isNull();
    }
}
