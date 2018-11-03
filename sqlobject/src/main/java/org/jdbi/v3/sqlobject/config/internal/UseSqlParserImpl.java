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
package org.jdbi.v3.sqlobject.config.internal;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.internal.Throwables;
import org.jdbi.v3.core.statement.SqlParser;
import org.jdbi.v3.core.statement.SqlStatements;
import org.jdbi.v3.sqlobject.config.Configurer;
import org.jdbi.v3.sqlobject.config.UseSqlParser;

public class UseSqlParserImpl implements Configurer {
    @Override
    public void configureForMethod(ConfigRegistry registry, Annotation annotation, Class<?> sqlObjectType, Method method) {
        UseSqlParser anno = (UseSqlParser) annotation;
        SqlParser parser = instantiate(anno.value(), sqlObjectType, method);
        registry.get(SqlStatements.class).setSqlParser(parser);
    }

    @Override
    public void configureForType(ConfigRegistry registry, Annotation annotation, Class<?> sqlObjectType) {
        configureForMethod(registry, annotation, sqlObjectType, null);
    }

    private SqlParser instantiate(Class<? extends SqlParser> parserClass,
                                  Class<?> sqlObjectType,
                                  @Nullable Method method) {
        return Stream.of(tryConstructor(parserClass), tryConstructor(parserClass, sqlObjectType), tryConstructor(parserClass, sqlObjectType, method))
            .map(Supplier::get)
            .filter(Objects::nonNull)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Unable to instantiate, no viable constructor for " + parserClass.getName()));
    }

    private static <T extends SqlParser> Supplier<T> tryConstructor(Class<T> clazz, Object... args) {
        return () -> {
            try {
                Object[] nonNullArgs = Arrays.stream(args).filter(Objects::nonNull).toArray(Object[]::new);
                Class[] argClasses = Arrays.stream(nonNullArgs).map(Object::getClass).toArray(Class[]::new);
                MethodType type = MethodType.methodType(void.class, argClasses);
                return (T) MethodHandles.lookup().findConstructor(clazz, type).invokeWithArguments(nonNullArgs);
            } catch (NoSuchMethodException ignored) {
                return null;
            } catch (Throwable t) {
                throw Throwables.throwOnlyUnchecked(t);
            }
        };
    }
}
