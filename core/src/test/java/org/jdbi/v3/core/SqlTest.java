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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SqlTest {

    @Test
    void testEmptyInput() {
        assertEquals("", Sql.of((CharSequence[]) null).toString());
        assertEquals("", Sql.of((CharSequence) null).toString());
        assertEquals("", Sql.of((Collection<CharSequence>) null).toString());
        assertEquals("", Sql.of(new CharSequence[0]).toString());
        assertEquals("", Sql.of(new ArrayList<>()).toString());
        assertEquals("", Sql.of().toString());
    }

    @Test
    void testMultipleStrings() {
        assertEquals("A B C", Sql.of(ImmutableList.of("A", "B", "C")).toString());

        assertEquals("A B C", Sql.of("A", "B", "C").toString());
    }

    @Test
    void testMultipleTokens() {
        assertEquals("SELECT COUNT(*) FROM table WHERE cond1 = :cond1",
                Sql.of("SELECT COUNT(*) FROM table ",
                       " WHERE cond1 = :cond1; ").toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "\n", "\t", ";"})
    @NullAndEmptySource
    void testStrip(String str) {
        Sql sql = Sql.of(str);
        assertEquals("", sql.toString());
    }

    @Test
    void testCharSequenceMethods() {
        Sql sql1 = Sql.of("SELECT * FROM table");
        assertEquals(19, sql1.length());
        assertEquals('S', sql1.charAt(0));
        assertEquals("SEL", sql1.subSequence(0, 3));
    }

    @Test
    void testEqualsAndHashCode() {
        Sql sql1 = Sql.of("DROP TABLE table;");
        Sql sql2 = Sql.of(sql1);
        assertEquals(sql1.hashCode(), sql2.hashCode());
        assertEquals(sql1, sql2);
        assertNotNull(sql1);
        assertNotEquals("", sql1.toString());
        assertNotEquals(sql1, Sql.of());
    }

}
