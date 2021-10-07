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
package org.jdbi.v3.guice.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.inject.Key;
import com.google.inject.PrivateModule;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import org.jdbi.v3.core.codec.Codec;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.spi.JdbiPlugin;
import org.jdbi.v3.guice.GuiceJdbiCustomizer;

import static com.google.common.base.Preconditions.checkNotNull;

public final class InternalGlobalJdbiModule extends PrivateModule {

    private static final TypeLiteral<Set<RowMapper<?>>> ROW_MAPPER_TYPE_LITERAL = new TypeLiteral<Set<RowMapper<?>>>() {};
    private static final TypeLiteral<Map<Type, RowMapper<?>>> QUALIFIED_ROW_MAPPER_TYPE_LITERAL = new TypeLiteral<Map<Type, RowMapper<?>>>() {};
    private static final TypeLiteral<Set<ColumnMapper<?>>> COLUMN_MAPPER_TYPE_LITERAL = new TypeLiteral<Set<ColumnMapper<?>>>() {};
    private static final TypeLiteral<Map<QualifiedType<?>, ColumnMapper<?>>> QUALIFIED_COLUMN_MAPPER_TYPE_LITERAL = new TypeLiteral<Map<QualifiedType<?>, ColumnMapper<?>>>() {};
    private static final TypeLiteral<Set<GuiceJdbiCustomizer>> CUSTOMIZER_TYPE_LITERAL = new TypeLiteral<Set<GuiceJdbiCustomizer>>() {};
    private static final TypeLiteral<Map<Class<?>, String>> ARRAY_TYPES_TYPE_LITERAL = new TypeLiteral<Map<Class<?>, String>>() {};
    private static final TypeLiteral<Set<JdbiPlugin>> PLUGINS_TYPE_LITERAL = new TypeLiteral<Set<JdbiPlugin>>() {};
    private static final TypeLiteral<Map<QualifiedType<?>, Codec<?>>> CODECS_TYPE_LITERAL = new TypeLiteral<Map<QualifiedType<?>, Codec<?>>>() {};

    private static final ConcurrentHashMap<Class<? extends Annotation>, InternalGlobalJdbiModule> REGISTRY = new ConcurrentHashMap<>();

    private final Class<? extends Annotation> annotation;

    public static InternalGlobalJdbiModule forAnnotation(Class<? extends Annotation> annotation) {
        checkNotNull(annotation, "annotation is null");
        return REGISTRY.computeIfAbsent(annotation, InternalGlobalJdbiModule::new);
    }

    private InternalGlobalJdbiModule(Class<? extends Annotation> annotation) {
        this.annotation = checkNotNull(annotation, "annotation is null");
    }

    @Override
    public void configure() {

        // these bindings exist because this module is called from AbstractJdbiConfigurationModule which instantiates
        // an InternalJdbiBinder that defines all the multibindings with the same annotation as this module.
        // So when guice tries to resolve the following bindings, all of them exist because they just have been defined
        binder().bind(ROW_MAPPER_TYPE_LITERAL).to(Key.get(ROW_MAPPER_TYPE_LITERAL, annotation));
        binder().bind(QUALIFIED_ROW_MAPPER_TYPE_LITERAL).to(Key.get(QUALIFIED_ROW_MAPPER_TYPE_LITERAL, annotation));
        binder().bind(COLUMN_MAPPER_TYPE_LITERAL).to(Key.get(COLUMN_MAPPER_TYPE_LITERAL, annotation));
        binder().bind(QUALIFIED_COLUMN_MAPPER_TYPE_LITERAL).to(Key.get(QUALIFIED_COLUMN_MAPPER_TYPE_LITERAL, annotation));
        binder().bind(CUSTOMIZER_TYPE_LITERAL).to(Key.get(CUSTOMIZER_TYPE_LITERAL, annotation));
        binder().bind(PLUGINS_TYPE_LITERAL).to(Key.get(PLUGINS_TYPE_LITERAL, annotation));
        binder().bind(ARRAY_TYPES_TYPE_LITERAL).to(Key.get(ARRAY_TYPES_TYPE_LITERAL, annotation));
        binder().bind(CODECS_TYPE_LITERAL).to(Key.get(CODECS_TYPE_LITERAL, annotation));

        // the previous binding statements bring the annotated bindings into the private module space without annotation
        // so that the InternalGuiceJdbiCustomizer can pick them up without a required annotation. Then in turn the created
        // customizer is exposed back out to the global binding namespace with the annotation passed in on the constructor.
        binder().bind(GuiceJdbiCustomizer.class).annotatedWith(annotation).to(InternalGuiceJdbiCustomizer.class).in(Scopes.SINGLETON);
        expose(Key.get(GuiceJdbiCustomizer.class, annotation));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        InternalGlobalJdbiModule that = (InternalGlobalJdbiModule) o;
        return annotation.equals(that.annotation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(annotation);
    }
}
