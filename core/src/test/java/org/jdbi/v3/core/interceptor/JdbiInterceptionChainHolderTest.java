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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JdbiInterceptionChainHolderTest {

    @Test
    public void testDefault() {
        JdbiInterceptionChainHolder<Object, Object> jdbiInterceptionChainHolder = new JdbiInterceptionChainHolder<>();
        Object source = new Object();
        assertThatThrownBy(() -> jdbiInterceptionChainHolder.process(source))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("object type 'Object' is not supported");
    }

    @Test
    public void testDefaultNull() {
        JdbiInterceptionChainHolder<Object, Object> jdbiInterceptionChainHolder = new JdbiInterceptionChainHolder<>();
        assertThatThrownBy(() -> jdbiInterceptionChainHolder.process(null))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("null value is not supported");
    }

    @Test
    public void testCustomTransformer() {
        Transformer<Object> t = new Transformer<>();

        JdbiInterceptionChainHolder<Object, String> holder = new JdbiInterceptionChainHolder<>(t);

        Object source = new Object();

        String result = holder.process(source);

        assertThat(source).isSameAs(t.getSource());
        assertThat(t.getTarget()).isEqualTo(result);
    }

    @Test
    public void testCustomTransformerNull() {
        Transformer<Object> t = new Transformer<>();

        JdbiInterceptionChainHolder<Object, String> holder = new JdbiInterceptionChainHolder<>(t);

        String result = holder.process(null);

        assertThat(t.getSource()).isNull();
        assertThat(t.getTarget()).isEqualTo(result);
    }

    @Test
    public void testFirstInterceptorSkip() {
        Transformer<Object> t = new Transformer<>();
        Interceptor<Object> i = new Interceptor<>(false);

        JdbiInterceptionChainHolder<Object, String> holder = new JdbiInterceptionChainHolder<>(t);
        holder.addFirst(i);

        Object source = new Object();
        String result = holder.process(source);

        assertThat(source).isSameAs(t.getSource())
                          .isSameAs(i.getSource());
        assertThat(t.getTarget()).isEqualTo(result);
        assertThat(i.getTarget()).isNotEqualTo(result);
    }

    @Test
    public void testFirstInterceptorHit() {
        Transformer<Object> t = new Transformer<>();
        Interceptor<Object> i = new Interceptor<>(true);

        JdbiInterceptionChainHolder<Object, String> holder = new JdbiInterceptionChainHolder<>(t);
        holder.addFirst(i);

        Object source = new Object();
        String result = holder.process(source);

        assertThat(source).isSameAs(i.getSource());
        assertThat(t.getSource()).isNull();
        assertThat(t.getTarget()).isNotEqualTo(result);
        assertThat(i.getTarget()).isEqualTo(result);
    }

    @Test
    public void testLastInterceptorHit() {
        Transformer<Object> t = new Transformer<>();
        Interceptor<Object> i = new Interceptor<>(true);

        JdbiInterceptionChainHolder<Object, String> holder = new JdbiInterceptionChainHolder<>(t);
        holder.addLast(i);

        Object source = new Object();
        String result = holder.process(source);

        assertThat(source).isSameAs(i.getSource());
        assertThat(t.getSource()).isNull();
        assertThat(t.getTarget()).isNotEqualTo(result);
        assertThat(i.getTarget()).isEqualTo(result);
    }

    @Test
    public void testLastInterceptorSkip() {
        Transformer<Object> t = new Transformer<>();
        Interceptor<Object> i = new Interceptor<>(false);

        JdbiInterceptionChainHolder<Object, String> holder = new JdbiInterceptionChainHolder<>(t);
        holder.addLast(i);

        Object source = new Object();
        String result = holder.process(source);

        assertThat(source).isSameAs(t.getSource())
                          .isSameAs(i.getSource());
        assertThat(t.getTarget()).isEqualTo(result);
        assertThat(i.getTarget()).isNotEqualTo(result);
    }

    @Test
    public void testFirstSkipHit() {
        Transformer<Object> t = new Transformer<>();
        Interceptor<Object> i1 = new Interceptor<>(true);
        Interceptor<Object> i2 = new Interceptor<>(false);

        JdbiInterceptionChainHolder<Object, String> holder = new JdbiInterceptionChainHolder<>(t);
        holder.addFirst(i1);
        holder.addFirst(i2);

        Object source = new Object();
        String result = holder.process(source);

        assertThat(source).isSameAs(i2.getSource())
                          .isSameAs(i1.getSource());
        assertThat(t.getSource()).isNull();

        assertThat(i1.getTarget()).isEqualTo(result);
        assertThat(i2.getTarget()).isNotEqualTo(result);
        assertThat(t.getTarget()).isNotEqualTo(result);
    }

    @Test
    public void testFirstHitSkip() {
        Transformer<Object> t = new Transformer<>();
        Interceptor<Object> i1 = new Interceptor<>(false);
        Interceptor<Object> i2 = new Interceptor<>(true);

        JdbiInterceptionChainHolder<Object, String> holder = new JdbiInterceptionChainHolder<>(t);
        holder.addFirst(i1);
        holder.addFirst(i2);

        Object source = new Object();
        String result = holder.process(source);

        assertThat(source).isSameAs(i2.getSource());
        assertThat(i1.getSource()).isNull();
        assertThat(t.getSource()).isNull();

        assertThat(i2.getTarget()).isEqualTo(result);
        assertThat(i1.getTarget()).isNotEqualTo(result);
        assertThat(t.getTarget()).isNotEqualTo(result);
    }

    @Test
    public void testLastSkipHit() {
        Transformer<Object> t = new Transformer<>();
        Interceptor<Object> i1 = new Interceptor<>(true);
        Interceptor<Object> i2 = new Interceptor<>(false);

        JdbiInterceptionChainHolder<Object, String> holder = new JdbiInterceptionChainHolder<>(t);
        holder.addLast(i1);
        holder.addLast(i2);

        Object source = new Object();
        String result = holder.process(source);

        assertThat(source).isSameAs(i1.getSource());
        assertThat(i2.getSource()).isNull();
        assertThat(t.getSource()).isNull();

        assertThat(i1.getTarget()).isEqualTo(result);
        assertThat(i2.getTarget()).isNotEqualTo(result);
        assertThat(t.getTarget()).isNotEqualTo(result);
    }

    @Test
    public void testLastHitSkip() {
        Transformer<Object> t = new Transformer<>();
        Interceptor<Object> i1 = new Interceptor<>(false);
        Interceptor<Object> i2 = new Interceptor<>(true);

        JdbiInterceptionChainHolder<Object, String> holder = new JdbiInterceptionChainHolder<>(t);
        holder.addLast(i1);
        holder.addLast(i2);

        Object source = new Object();
        String result = holder.process(source);

        assertThat(source).isSameAs(i2.getSource())
                          .isSameAs(i1.getSource());
        assertThat(t.getSource()).isNull();

        assertThat(i2.getTarget()).isEqualTo(result);
        assertThat(i1.getTarget()).isNotEqualTo(result);
        assertThat(t.getTarget()).isNotEqualTo(result);
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

        @Override
        public String intercept(S s, JdbiInterceptionChain<String> chain) {
            this.source = s;
            if (doIt) {
                return this.target;
            } else {
                return chain.next();
            }
        }
    }
}
