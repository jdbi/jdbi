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
package org.jdbi.v3.postgres;

import java.sql.SQLException;
import java.util.Objects;

import org.postgresql.util.PGobject;
import org.postgresql.util.PGtokenizer;

public class FooBarPGType extends PGobject {

    private Integer id;
    private String foo;
    private String bar;

    public FooBarPGType(Integer id, String foo, String bar) {
        this();
        this.id = id;
        this.foo = foo;
        this.bar = bar;
    }

    public FooBarPGType() {
        setType("foo_bar_type");
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getFoo() {
        return foo;
    }

    public void setFoo(String foo) {
        this.foo = foo;
    }

    public String getBar() {
        return bar;
    }

    public void setBar(String bar) {
        this.bar = bar;
    }

    @Override
    public String getValue() {
        return "(" + id + "," + foo + "," + bar + ")";
    }

    @Override
    public void setValue(String value) throws SQLException {
        PGtokenizer t = new PGtokenizer(PGtokenizer.removePara(value), ',');

        id = Integer.valueOf(t.getToken(0));
        foo = t.getToken(1);
        bar = t.getToken(2);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 43 * hash + Objects.hashCode(this.id);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final FooBarPGType other = (FooBarPGType) obj;
        if (!Objects.equals(this.foo, other.foo)) {
            return false;
        }
        if (!Objects.equals(this.bar, other.bar)) {
            return false;
        }
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        return true;
    }

}
