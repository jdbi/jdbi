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

import java.util.ArrayList;
import java.util.Collection;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

public class SqlTest {

    @Test
    void testEmptyInput() {
        assertThat(Sql.of((CharSequence[]) null).toString()).isBlank();
        assertThat(Sql.of((CharSequence) null).toString()).isBlank();
        assertThat(Sql.of((Collection<CharSequence>) null).toString()).isBlank();
        assertThat(Sql.of(new CharSequence[0]).toString()).isBlank();
        assertThat(Sql.of(new ArrayList<>()).toString()).isBlank();
        assertThat(Sql.of().toString()).isBlank();
    }

    @Test
    void testMultipleStrings() {
        assertThat(Sql.of(ImmutableList.of("A", "B", "C")))
                .hasToString("A B C");
        assertThat(Sql.of("A", "B", "C"))
                .hasToString("A B C");
    }

    @Test
    void testMultipleTokens() {
        assertThat(Sql.of("SELECT COUNT(*) FROM table",
                          "WHERE cond1 = :cond1; "))
                        .hasToString("SELECT COUNT(*) FROM table WHERE cond1 = :cond1");
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "\n", "\t", ";"})
    @NullAndEmptySource
    void testStrip(String str) {
        Sql sql = Sql.of(str);
        assertThat(sql).hasToString("");
    }

    @Test
    void testCharSequenceMethods() {
        Sql sql = Sql.of("SELECT * FROM table");
        assertThat(sql).hasSize(19);
        assertThat(sql.charAt(0)).isEqualTo('S');
        assertThat(sql.subSequence(0, 3)).isEqualTo("SEL");
    }

    @Test
    void testEqualsAndHashCode() {
        Sql sql1 = Sql.of("DROP TABLE table;");
        Sql sql2 = Sql.of(sql1);
        assertThat(sql1).isNotNull()
                        .isEqualTo(sql2)
                        .hasSameHashCodeAs(sql2)
                        .isNotEqualTo(Sql.of());
        assertThat(sql1.toString()).isNotBlank();
    }

}
