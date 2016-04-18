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
package org.jdbi.v3.sqlobject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.jdbi.v3.H2DatabaseRule;
import org.jdbi.v3.Something;
import org.jdbi.v3.sqlobject.customizers.UseRowMapper;
import org.jdbi.v3.sqlobject.mixins.GetHandle;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TestVariousOddities
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void testAttach() throws Exception
    {
        Spiffy s = db.getSharedHandle().attach(Spiffy.class);
        s.insert(new Something(14, "Tom"));

        Something tom = s.byId(14);
        assertEquals("Tom", tom.getName());
    }

    @Test
    public void testEquals()
    {
        Spiffy s1 = db.getSharedHandle().attach(Spiffy.class);
        Spiffy s2 = db.getSharedHandle().attach(Spiffy.class);
        assertEquals(s1, s1);
        assertNotSame(s1, s2);
        assertFalse(s1.equals(s2));
    }

    @Test
    public void testToString()
    {
        Spiffy s1 = db.getSharedHandle().attach(Spiffy.class);
        Spiffy s2 = db.getSharedHandle().attach(Spiffy.class);
        assertNotNull(s1.toString());
        assertNotNull(s2.toString());
        assertTrue(s1.toString() != s2.toString());
    }

    @Test
    public void testHashCode()
    {
        Spiffy s1 = db.getSharedHandle().attach(Spiffy.class);
        Spiffy s2 = db.getSharedHandle().attach(Spiffy.class);
        assertFalse(0 == s1.hashCode());
        assertFalse(0 == s2.hashCode());
        assertTrue(s1.hashCode() != s2.hashCode());
    }

    @Test
    public void testConcurrentHashCode() throws ExecutionException, InterruptedException
    {
        Callable<SpiffyConcurrent> callable = () ->
                db.getSharedHandle().attach(SpiffyConcurrent.class);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        Future<SpiffyConcurrent> f1 = pool.submit(callable);
        Future<SpiffyConcurrent> f2 = pool.submit(callable);
        SpiffyConcurrent s1 = f1.get();
        SpiffyConcurrent s2 = f2.get();
        assertFalse(0 == s1.hashCode());
        assertFalse(0 == s2.hashCode());
        assertTrue(s1.hashCode() != s2.hashCode());
    }

    @Test
    public void testNullQueryReturn()
    {
        exception.expect(IllegalStateException.class);
        exception.expectMessage(
                "Method org.jdbi.v3.sqlobject.TestVariousOddities$SpiffyBoom#returnNothing " +
                        "is annotated as if it should return a value, but the method is void.");

        db.getSharedHandle().attach(SpiffyBoom.class);
    }

    public interface Spiffy
    {
        @SqlQuery("select id, name from something where id = :id")
        @UseRowMapper(SomethingMapper.class)
        Something byId(@Bind("id") long id);

        @SqlUpdate("insert into something (id, name) values (:it.id, :it.name)")
        void insert(@Bind(value = "it", binder = SomethingBinderAgainstBind.class) Something it);

    }

    public interface SpiffyBoom
    {
        @SqlQuery("SELECT 1")
        void returnNothing();
    }

    /**
     * This interface should not be loaded by any test other than {@link TestVariousOddities#testConcurrentHashCode()}.
     */
    public interface SpiffyConcurrent extends GetHandle
    {

    }
}
