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
package org.jdbi.spring.jta;

import java.util.List;

import org.jdbi.core.result.ResultIterator;
import org.jdbi.spring.JdbiRepository;
import org.jdbi.sqlobject.statement.SqlQuery;

@JdbiRepository
public interface SomethingDao {
    @SqlQuery("select name from something")
    ResultIterator<String> queryReturningResultIterator();

    @SqlQuery("select name from something")
    List<String> queryReturningList();

    @SqlQuery("select name from something where 1/0 = 1")
    String exceptionThrowingQuery();

}
