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
package org.jdbi.core;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jdbi.core.extension.ExtensionContext;
import org.jdbi.core.extension.ExtensionMethod;
import org.jdbi.core.extension.HandleSupplier;
import org.jdbi.core.internal.testing.H2DatabaseExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The extension context that an extension call binds to a handle is visible only to the thread that makes the call.
 */
public class TestHandleExtensionContextThreads {

    private static final long TIMEOUT_SECONDS = 10;

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance();

    private Handle handle;
    private ExecutorService executor;
    private ExtensionContext contextA;
    private ExtensionContext contextB;

    @BeforeEach
    public void setUp() throws Exception {
        handle = h2Extension.openHandle();
        executor = Executors.newFixedThreadPool(2);
        contextA = ExtensionContext.forExtensionMethod(handle.configRegistry(), Runnable.class, Runnable.class.getMethod("run"));
        contextB = ExtensionContext.forExtensionMethod(handle.configRegistry(), Callable.class, Callable.class.getMethod("call"));
    }

    @AfterEach
    public void tearDown() {
        executor.shutdownNow();
        handle.close();
    }

    @Test
    public void testContextIsNotVisibleToOtherThreads() throws Exception {
        final CountDownLatch aEntered = new CountDownLatch(1);
        final CountDownLatch bDone = new CountDownLatch(1);

        final Future<ExtensionMethod> a = executor.submit(() -> ConstantHandleSupplier.of(handle).invokeInContext(contextA, () -> {
            aEntered.countDown();
            await(bDone);
            return handle.getExtensionMethod();
        }));
        await(aEntered);

        assertThat(handle.getExtensionMethod()).isNull();

        final Future<ExtensionMethod> b = executor.submit(() -> ConstantHandleSupplier.of(handle).invokeInContext(contextB, handle::getExtensionMethod));
        assertThat(b.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isSameAs(contextB.getExtensionMethod());
        bDone.countDown();

        assertThat(a.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isSameAs(contextA.getExtensionMethod());
        assertThat(handle.getExtensionMethod()).isNull();
    }

    @Test
    public void testSharedSupplierKeepsContextPerThread() throws Exception {
        final HandleSupplier supplier = ConstantHandleSupplier.of(handle);
        final CountDownLatch aEntered = new CountDownLatch(1);
        final CountDownLatch bEntered = new CountDownLatch(1);
        final CountDownLatch aDone = new CountDownLatch(1);

        final Future<ExtensionMethod> a = executor.submit(() -> supplier.invokeInContext(contextA, () -> {
            aEntered.countDown();
            await(bEntered);
            return handle.getExtensionMethod();
        }));
        await(aEntered);

        final Future<ExtensionMethod> b = executor.submit(() -> supplier.invokeInContext(contextB, () -> {
            bEntered.countDown();
            await(aDone);
            return handle.getExtensionMethod();
        }));

        assertThat(a.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isSameAs(contextA.getExtensionMethod());
        aDone.countDown();

        assertThat(b.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isSameAs(contextB.getExtensionMethod());
    }

    @Test
    public void testNestedContextIsRestored() throws Exception {
        final HandleSupplier supplier = ConstantHandleSupplier.of(handle);

        final ExtensionMethod afterInner = supplier.invokeInContext(contextA, () -> {
            final ExtensionMethod inner = supplier.invokeInContext(contextB, handle::getExtensionMethod);
            assertThat(inner).isSameAs(contextB.getExtensionMethod());
            return handle.getExtensionMethod();
        });

        assertThat(afterInner).isSameAs(contextA.getExtensionMethod());
        assertThat(handle.getExtensionMethod()).isNull();
    }

    private static void await(CountDownLatch latch) throws InterruptedException {
        assertThat(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
    }
}
