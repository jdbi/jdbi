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
package org.jdbi.v3.core.interceptor;

import java.util.UUID;
import java.util.function.Function;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JdbiInterceptionChainHolderTest {

    @Test
    public void testDefault() {
        JdbiInterceptionChainHolder<Object, Object> holder = new JdbiInterceptionChainHolder<>();

        UnsupportedOperationException e = assertThrows(UnsupportedOperationException.class, () -> holder.process(new Object()));

        assertEquals("object type 'Object' is not supported", e.getMessage());
    }

    @Test
    public void testDefaultNull() {
        JdbiInterceptionChainHolder<Object, Object> holder = new JdbiInterceptionChainHolder<>();

        UnsupportedOperationException e = assertThrows(UnsupportedOperationException.class, () -> holder.process(null));

        assertEquals("null value is not supported", e.getMessage());
    }

    @Test
    public void testCustomTransformer() {
        Transformer<Object> t = new Transformer();

        JdbiInterceptionChainHolder<Object, String> holder = new JdbiInterceptionChainHolder(t);

        Object source = new Object();

        String result = holder.process(source);

        assertSame(source, t.getSource());
        assertEquals(t.getTarget(), result);
    }

    @Test
    public void testCustomTransformerNull() {
        Transformer<Object> t = new Transformer();

        JdbiInterceptionChainHolder<Object, String> holder = new JdbiInterceptionChainHolder(t);

        String result = holder.process(null);

        assertNull(t.getSource());
        assertEquals(t.getTarget(), result);
    }

    @Test
    public void testFirstInterceptorSkip() {
        Transformer<Object> t = new Transformer();
        Interceptor<Object> i = new Interceptor<>(false);

        JdbiInterceptionChainHolder<Object, String> holder = new JdbiInterceptionChainHolder(t);
        holder.addFirst(i);

        Object source = new Object();
        String result = holder.process(source);

        assertSame(source, t.getSource());
        assertSame(source, i.getSource());
        assertEquals(t.getTarget(), result);
        assertNotEquals(i.getTarget(), result);
    }

    @Test
    public void testFirstInterceptorHit() {
        Transformer<Object> t = new Transformer();
        Interceptor<Object> i = new Interceptor<>(true);

        JdbiInterceptionChainHolder<Object, String> holder = new JdbiInterceptionChainHolder(t);
        holder.addFirst(i);

        Object source = new Object();
        String result = holder.process(source);

        assertSame(source, i.getSource());
        assertNull(t.getSource());
        assertNotEquals(t.getTarget(), result);
        assertEquals(i.getTarget(), result);
    }

    @Test
    public void testLastInterceptorHit() {
        Transformer<Object> t = new Transformer();
        Interceptor<Object> i = new Interceptor<>(true);

        JdbiInterceptionChainHolder<Object, String> holder = new JdbiInterceptionChainHolder(t);
        holder.addLast(i);

        Object source = new Object();
        String result = holder.process(source);

        assertSame(source, i.getSource());
        assertNull(t.getSource());
        assertNotEquals(t.getTarget(), result);
        assertEquals(i.getTarget(), result);
    }

    @Test
    public void testLastInterceptorSkip() {
        Transformer<Object> t = new Transformer();
        Interceptor<Object> i = new Interceptor<>(false);

        JdbiInterceptionChainHolder<Object, String> holder = new JdbiInterceptionChainHolder(t);
        holder.addLast(i);

        Object source = new Object();
        String result = holder.process(source);

        assertSame(source, t.getSource());
        assertSame(source, i.getSource());
        assertEquals(t.getTarget(), result);
        assertNotEquals(i.getTarget(), result);
    }

    @Test
    public void testFirstSkipHit() {
        Transformer<Object> t = new Transformer();
        Interceptor<Object> i1 = new Interceptor<>(true);
        Interceptor<Object> i2 = new Interceptor<>(false);

        JdbiInterceptionChainHolder<Object, String> holder = new JdbiInterceptionChainHolder(t);
        holder.addFirst(i1);
        holder.addFirst(i2);

        Object source = new Object();
        String result = holder.process(source);

        assertSame(source, i2.getSource());
        assertSame(source, i1.getSource());
        assertNull(t.getSource());

        assertEquals(i1.getTarget(), result);
        assertNotEquals(i2.getTarget(), result);
        assertNotEquals(t.getTarget(), result);
    }

    @Test
    public void testFirstHitSkip() {
        Transformer<Object> t = new Transformer();
        Interceptor<Object> i1 = new Interceptor<>(false);
        Interceptor<Object> i2 = new Interceptor<>(true);

        JdbiInterceptionChainHolder<Object, String> holder = new JdbiInterceptionChainHolder(t);
        holder.addFirst(i1);
        holder.addFirst(i2);

        Object source = new Object();
        String result = holder.process(source);

        assertSame(source, i2.getSource());
        assertNull(i1.getSource());
        assertNull(t.getSource());

        assertEquals(i2.getTarget(), result);
        assertNotEquals(i1.getTarget(), result);
        assertNotEquals(t.getTarget(), result);
    }

    @Test
    public void testLastSkipHit() {
        Transformer<Object> t = new Transformer();
        Interceptor<Object> i1 = new Interceptor<>(true);
        Interceptor<Object> i2 = new Interceptor<>(false);

        JdbiInterceptionChainHolder<Object, String> holder = new JdbiInterceptionChainHolder(t);
        holder.addLast(i1);
        holder.addLast(i2);

        Object source = new Object();
        String result = holder.process(source);

        assertSame(source, i1.getSource());
        assertNull(i2.getSource());
        assertNull(t.getSource());

        assertEquals(i1.getTarget(), result);
        assertNotEquals(i2.getTarget(), result);
        assertNotEquals(t.getTarget(), result);
    }

    @Test
    public void testLastHitSkip() {
        Transformer<Object> t = new Transformer();
        Interceptor<Object> i1 = new Interceptor<>(false);
        Interceptor<Object> i2 = new Interceptor<>(true);

        JdbiInterceptionChainHolder<Object, String> holder = new JdbiInterceptionChainHolder(t);
        holder.addLast(i1);
        holder.addLast(i2);

        Object source = new Object();
        String result = holder.process(source);

        assertSame(source, i2.getSource());
        assertSame(source, i1.getSource());
        assertNull(t.getSource());

        assertEquals(i2.getTarget(), result);
        assertNotEquals(i1.getTarget(), result);
        assertNotEquals(t.getTarget(), result);
    }

    static class Transformer<S> implements Function<S, String> {

        private S source;
        private final String target = UUID.randomUUID().toString();

        S getSource() {
            return source;
        }

        String getTarget() {
            return target;
        }

        @Override
        public String apply(S s) {
            this.source = s;
            return this.target;
        }
    }

    static class Interceptor<S> implements JdbiInterceptor<S, String> {

        private S source;
        private final String target = UUID.randomUUID().toString();
        private final boolean doIt;

        Interceptor(boolean doIt) {
            this.doIt = doIt;
        }

        S getSource() {
            return source;
        }

        String getTarget() {
            return target;
        }

        @CheckForNull
        @Override
        public String intercept(@Nullable S s, JdbiInterceptionChain<String> chain) {
            this.source = s;
            if (doIt) {
                return this.target;
            } else {
                return chain.next();
            }
        }
    }
}
