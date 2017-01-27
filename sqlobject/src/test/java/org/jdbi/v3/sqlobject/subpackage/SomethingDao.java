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
package org.jdbi.v3.sqlobject.subpackage;

import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transaction;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;

@RegisterRowMapper(SomethingMapper.class)
public interface SomethingDao
{
    @SqlUpdate("insert into something (id, name) values (:id, :name)")
    void insert(@Bind("id") int id, @Bind("name") String name);

    @SqlQuery("select id, name from something where id = :id")
    Something findById(@Bind("id") int id);

    default Something findByIdHeeHee(int id) {
        return findById(id);
    }

    @Transaction
    default void insertInSingleTransaction(final int id, final String name)
    {
        insert(id, name);
    }

    @Transaction
    default void insertInNestedTransaction(final int id, final String name)
    {
        insertInSingleTransaction(id, name);
    }


}
