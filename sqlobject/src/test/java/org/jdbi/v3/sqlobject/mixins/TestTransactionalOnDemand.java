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
package org.jdbi.v3.sqlobject.mixins;

import org.jdbi.v3.core.H2DatabaseRule;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestTransactionalOnDemand
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    Txn txn;

    @Before
    public void setUp()
    {
        txn = db.getJdbi().onDemand(Txn.class);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testBegin() throws Exception
    {
        txn.begin();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testCommit() throws Exception
    {
        txn.commit();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRollback() throws Exception
    {
        txn.rollback();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSavepoint() throws Exception
    {
        txn.savepoint("somewhere");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRollbackToSavepoint() throws Exception
    {
        txn.rollbackToSavepoint("somewhere");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testReleaseSavepoint() throws Exception
    {
        txn.releaseSavepoint("somewhere");
    }

    public interface Txn extends Transactional<Txn>
    {

    }
}
