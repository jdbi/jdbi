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
package org.jdbi.spring5.jta;

import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SomethingService {
    private final SomethingDao somethingDao;

    @Autowired
    public SomethingService(SomethingDao somethingDao) {
        this.somethingDao = somethingDao;
    }

    @Transactional
    public void inTransaction(Function<SomethingDao, ?> function) {
        var unused = function.apply(somethingDao);
    }

    public void withoutTransaction(Function<SomethingDao, ?> function) {
        var unused = function.apply(somethingDao);
    }

}
