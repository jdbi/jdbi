/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nebhale.r2dbc;

/**
 * SQL transaction isolation levels.
 */
public enum IsolationLevel {

    /**
     * The read committed isolation level.
     */
    READ_COMMITTED("READ COMMITTED"),

    /**
     * The read uncommitted isolation level.
     */
    READ_UNCOMMITTED("READ UNCOMMITTED"),

    /**
     * THe repeatable read isolation level.
     */
    REPEATABLE_READ("REPEATABLE READ"),

    /**
     * The serializable isolation level.
     */
    SERIALIZABLE("SERIALIZABLE");

    private final String sql;

    IsolationLevel(String sql) {
        this.sql = sql;
    }

    /**
     * Returns the SQL string represented be each value.
     *
     * @return the SQL string represented be each value
     */
    public String asSql() {
        return this.sql;
    }

    @Override
    public String toString() {
        return "IsolationLevel{" +
            "sql='" + this.sql + '\'' +
            "} " + super.toString();
    }

}

