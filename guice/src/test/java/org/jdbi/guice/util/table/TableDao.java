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
package org.jdbi.guice.util.table;

import java.util.List;

import org.jdbi.core.Jdbi;
import org.jdbi.sqlobject.customizer.BindBean;
import org.jdbi.sqlobject.customizer.Define;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.sqlobject.statement.SqlUpdate;

public final class TableDao {

    private final Instance instance;
    private final String tableName;

    static TableDao getDao(Jdbi jdbi, String tableName) {
        return new TableDao(jdbi, tableName);
    }

    private TableDao(Jdbi jdbi, String tableName) {
        this.tableName = tableName;
        this.instance = jdbi.onDemand(Instance.class);
    }

    public int insert(Table data) {
        return instance.insert(tableName, data);
    }

    public List<Table> select() {
        return instance.select(tableName);
    }

    interface Instance {

        @SqlUpdate("INSERT INTO <table_name> (u, s, j) VALUES (:uuid, :s, :j::json)")
        int insert(@Define("table_name") String tableName, @BindBean Table data);

        @SqlQuery("SELECT * FROM <table_name>")
        List<Table> select(@Define("table_name") String tableName);

    }
}
