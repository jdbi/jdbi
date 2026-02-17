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
package org.jdbi.sqlobject.subpackage;

import org.jdbi.core.Something;
import org.jdbi.core.mapper.SomethingMapper;
import org.jdbi.sqlobject.config.RegisterRowMapper;
import org.jdbi.sqlobject.customizer.Bind;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.sqlobject.statement.SqlUpdate;
import org.jdbi.sqlobject.transaction.Transaction;

@RegisterRowMapper(SomethingMapper.class)
public interface SomethingDao {
    @SqlUpdate("insert into something (id, name) values (:id, :name)")
    int insert(@Bind("id") int id, @Bind("name") String name);

    @SqlQuery("select id, name from something where id = :id")
    Something findById(@Bind("id") int id);

    default Something findByIdHeeHee(int id) {
        return findById(id);
    }

    @Transaction
    default int insertInSingleTransaction(final int id, final String name) {
        return insert(id, name);
    }

    @Transaction
    default int insertInNestedTransaction(final int id, final String name) {
        return insertInSingleTransaction(id, name);
    }

}
