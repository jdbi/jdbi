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
package org.jdbi.v3.guice.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import com.google.common.base.MoreObjects;
import com.google.inject.Inject;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class MyString {

    public static MyString fromString(String s) {
        return new MyString(s);
    }

    private final String s;

    private MyString(String s) {
        this.s = s;
    }

    public String getValue() {
        return s;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MyString myString = (MyString) o;
        return Objects.equals(s, myString.s);
    }

    @Override
    public int hashCode() {
        return Objects.hash(s);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("s", s)
            .toString();
    }

    public static class MyStringColumnMapper implements ColumnMapper<MyString> {

        @Inject
        public MyStringColumnMapper() {}

        @Override
        public MyString map(ResultSet rs, int c, StatementContext ctx) throws SQLException {
            return MyString.fromString(rs.getString(c));
        }
    }
}
