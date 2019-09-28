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
package org.jdbi.v3.core.argument.internal;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.BeanPropertyArguments;
import org.jdbi.v3.core.argument.ObjectFieldArguments;
import org.jdbi.v3.core.argument.ObjectMethodArguments;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.mapper.reflect.internal.PojoProperties;
import org.jdbi.v3.core.mapper.reflect.internal.PojoProperties.PojoProperty;
import org.jdbi.v3.core.qualifier.QualifiedType;

@SuppressWarnings("deprecation")
public interface NamedArgumentFinderFactory<T> {
    NamedArgumentFinderFactory<Object> BEAN = new Bean<>();
    NamedArgumentFinderFactory<Object> POJO = new Pojo<>();
    NamedArgumentFinderFactory<Object> FIELDS = new Fields<>();
    NamedArgumentFinderFactory<Object> METHODS = new Methods<>();

    PrepareKey keyFor(String prefix, Object value);
    Function<String, Optional<Function<Object, Argument>>> prepareFor(
            ConfigRegistry config,
            Function<QualifiedType<?>, Function<Object, Argument>> argumentFactoryLookup,
            String prefix, Object example);

    abstract class Base<T, PrepareData> implements NamedArgumentFinderFactory<T> {
        @Override
        public PrepareKey keyFor(String prefix, Object value) {
            return new PrepareKey(getClass(), value.getClass(), prefix);
        }
    }

    class Pojo<T> extends Base<T, PojoProperties<T>> {
        @Override
        public Function<String, Optional<Function<Object, Argument>>> prepareFor(
                ConfigRegistry config,
                Function<QualifiedType<?>, Function<Object, Argument>> argumentFactoryLookup,
                String prefix, Object example) {
            return forPojoProps(argumentFactoryLookup, new PojoPropertyArguments(prefix, example, config));
        }

        Function<String, Optional<Function<Object, Argument>>> forPojoProps(
                Function<QualifiedType<?>, Function<Object, Argument>> argumentFactoryLookup,
                PojoPropertyArguments ppa) {
            return name -> Optional.ofNullable(ppa.properties.getProperties().get(name))
                    .map(PojoProperty.class::cast)
                    .map(property -> {
                        Function<Object, Argument> arg = argumentFactoryLookup.apply(property.getQualifiedType());
                        return pojo -> arg.apply(property.get(pojo));
                    });
        }
    }

    class Bean<T> extends Pojo<T> {
        @Override
        public Function<String, Optional<Function<Object, Argument>>> prepareFor(
                ConfigRegistry config,
                Function<QualifiedType<?>, Function<Object, Argument>> argumentFactoryLookup,
                String prefix, Object example) {
            return forPojoProps(argumentFactoryLookup, new BeanPropertyArguments(prefix, example, config));
        }
    }

    abstract class ReflectionBase<T> extends Base<T, PojoProperties<T>> {
        @Override
        public Function<String, Optional<Function<Object, Argument>>> prepareFor(
                ConfigRegistry config,
                Function<QualifiedType<?>, Function<Object, Argument>> argumentFactoryLookup,
                String prefix, Object example) {
            return name -> create().apply(prefix, example).apply(name, config)
                    .map(getter -> {
                        Function<Object, Argument> arg = argumentFactoryLookup.apply(getter.apply(example).getType());
                        return obj -> arg.apply(getter.apply(obj).getValue());
                    });
        }
        abstract BiFunction<String, Object, BiFunction<String, ConfigRegistry, Optional<Function<Object, TypedValue>>>> create();
    }

    class Fields<T> extends ReflectionBase<T> {
        @Override
        BiFunction<String, Object, BiFunction<String, ConfigRegistry, Optional<Function<Object, TypedValue>>>> create() {
            return (prefix, example) -> new ObjectFieldArguments(prefix, example)::getter;
        }
    }

    class Methods<T> extends ReflectionBase<T> {
        @Override
        BiFunction<String, Object, BiFunction<String, ConfigRegistry, Optional<Function<Object, TypedValue>>>> create() {
            return (prefix, example) -> new ObjectMethodArguments(prefix, example)::getter;
        }
    }

    class PrepareKey {
        final Class<?> factoryClass;
        final Type type;
        final String prefix;

        PrepareKey(Class<?> factoryClass, Type type, String prefix) {
            this.factoryClass = factoryClass;
            this.type = type;
            this.prefix = prefix;
        }

        @Override
        public int hashCode() {
            return Objects.hash(factoryClass, prefix, type);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            PrepareKey other = (PrepareKey) obj;
            return Objects.equals(factoryClass, other.factoryClass)
                    && Objects.equals(prefix, other.prefix)
                    && Objects.equals(type, other.type);
        }
    }
}
