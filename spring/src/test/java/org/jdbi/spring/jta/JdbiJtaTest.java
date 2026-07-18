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

import java.util.ArrayList;
import java.util.List;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

import org.jdbi.core.result.ResultIterator;
import org.jdbi.core.statement.UnableToCreateStatementException;
import org.jdbi.testing.internal.JdbiLeakChecker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { JdbiJtaTestConfiguration.class })
public class JdbiJtaTest {

    @Autowired
    private SomethingService somethingService;

    // attached to the (immutable) Jdbi at assembly time via JdbiLeakCheckerPlugin in test-context.xml
    @Autowired
    private JdbiLeakChecker jdbiLeakChecker;

    @Test
    void testQueryWithoutTxDoesNotLeak() {
        somethingService.withoutTransaction(SomethingDao::queryReturningList);
        jdbiLeakChecker.checkForLeaks();
    }

    @Test
    void testQueryInTxDoesNotLeak() {
        somethingService.inTransaction(SomethingDao::queryReturningList);
        jdbiLeakChecker.checkForLeaks();
    }

    @Test
    void testIteratorInTxDoesNotLeak() {
        somethingService.inTransaction(somethingDao -> {
            List<String> result = new ArrayList<>();
            ResultIterator<String> iterator = somethingDao.queryReturningResultIterator();
            StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false).forEach(result::add);
            return result;
        });
        jdbiLeakChecker.checkForLeaks();
    }

    @Test
    void testExceptionInTxDoesNotLeak() {
        Assertions.assertThrows(UnableToCreateStatementException.class,
                () -> somethingService.inTransaction(SomethingDao::exceptionThrowingQuery)
        );
        jdbiLeakChecker.checkForLeaks();
    }
}
