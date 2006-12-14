/*
 * Copyright 2004-2006 Brian McCallister
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

package org.skife.jdbi.v2.unstable.eod;

import static org.skife.jdbi.v2.unstable.eod.BindType.Bean;
import org.skife.jdbi.v2.ResultIterator;
import org.skife.jdbi.v2.Something;

import java.util.List;

/**
 *
 */
public interface MyQueries extends DataAccessor
{
    @Select("select id, name from something")
    List<Something> getAllSomethings();

    @Select("select id, name from something where name = ?")
    List<Something> findByName(String name);

    @Select("select id, name from something")
    ResultIterator<Something> ittyAll();

    @Select("select id, name from something where id = :id")
    Something findById(int i);

    @Insert("insert into something (id, name) values (:id, :name)")
    boolean insert(int id, String name);

    @Update("update something set name=? where id=?")
    int updateNameById(String name, int id);

    @Delete("delete from something where id = ?")
    boolean deleteById(int id);

    @Insert("insert into something (id, name) values (:id, :name)")
    @BindBy(Bean)
    boolean insert(Something s);
}
