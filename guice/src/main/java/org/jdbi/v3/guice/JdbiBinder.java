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
package org.jdbi.v3.guice;

import java.lang.reflect.Type;

import com.google.inject.binder.LinkedBindingBuilder;
import org.jdbi.v3.core.codec.Codec;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.spi.JdbiPlugin;

/**
 * Describes all operations to bind Jdbi elements in Guice.
 */
public interface JdbiBinder {

    /**
     * Provides access to a {@link JdbiBinder} instance.
     * <p>
     * Must be overridden by implementing classes, otherwise it will throw {@link UnsupportedOperationException}.
     */
    default JdbiBinder jdbiBinder() {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a new binding for a {@link RowMapper}.
     * <p>
     * <pre>
     *   jdbiBinder.bindRowMapper().to(FancyDataTypeMapper.class).in(Scopes.SINGLETON);
     *   jdbiBinder.bindRowMapper().toInstance(new BoringDataTypeMapper()).in(Scopes.SINGLETON);
     * </pre>
     * <p>
     * Only valid when called from {@link AbstractJdbiDefinitionModule#configureJdbi()} or {@link AbstractJdbiConfigurationModule#configureJdbi()}.
     */
    default LinkedBindingBuilder<RowMapper<?>> bindRowMapper() {
        return jdbiBinder().bindRowMapper();
    }

    /**
     * Creates a new binding for a {@link RowMapper} using a {@link GenericType}.
     * <p>
     * Only valid when called from {@link AbstractJdbiDefinitionModule#configureJdbi()} or {@link AbstractJdbiConfigurationModule#configureJdbi()}.
     */
    default LinkedBindingBuilder<RowMapper<?>> bindRowMapper(GenericType<?> genericType) {
        return jdbiBinder().bindRowMapper(genericType);
    }

    /**
     * Creates a new binding for a {@link RowMapper} using a {@link Type}.
     * <p>
     * Only valid when called from {@link AbstractJdbiDefinitionModule#configureJdbi()} or {@link AbstractJdbiConfigurationModule#configureJdbi()}.
     */
    default LinkedBindingBuilder<RowMapper<?>> bindRowMapper(Type type) {
        return jdbiBinder().bindRowMapper(type);
    }

    /**
     * Creates a new binding for a {@link ColumnMapper}.
     * <p>
     * Only valid when called from {@link AbstractJdbiDefinitionModule#configureJdbi()} or {@link AbstractJdbiConfigurationModule#configureJdbi()}.
     */
    default LinkedBindingBuilder<ColumnMapper<?>> bindColumnMapper() {
        return jdbiBinder().bindColumnMapper();
    }

    /**
     * Creates a new binding for a {@link ColumnMapper} using a {@link QualifiedType}.
     * <p>
     * Only valid when called from {@link AbstractJdbiDefinitionModule#configureJdbi()} or {@link AbstractJdbiConfigurationModule#configureJdbi()}.
     */
    default LinkedBindingBuilder<ColumnMapper<?>> bindColumnMapper(QualifiedType<?> qualifiedType) {
        return jdbiBinder().bindColumnMapper(qualifiedType);
    }

    /**
     * Creates a new binding for a {@link ColumnMapper} using a {@link GenericType}.
     * <p>
     * Only valid when called from {@link AbstractJdbiDefinitionModule#configureJdbi()} or {@link AbstractJdbiConfigurationModule#configureJdbi()}.
     */
    default LinkedBindingBuilder<ColumnMapper<?>> bindColumnMapper(GenericType<?> genericType) {
        return jdbiBinder().bindColumnMapper(genericType);
    }

    /**
     * Creates a new binding for a {@link ColumnMapper} using a {@link Type}.
     * <p>
     * Only valid when called from {@link AbstractJdbiDefinitionModule#configureJdbi()} or {@link AbstractJdbiConfigurationModule#configureJdbi()}.
     */
    default LinkedBindingBuilder<ColumnMapper<?>> bindColumnMapper(Type type) {
        return jdbiBinder().bindColumnMapper(type);
    }

    /**
     * Creates a new binding for a {@link Codec} using a {@link QualifiedType}.
     * <p>
     * Only valid when called from {@link AbstractJdbiDefinitionModule#configureJdbi()} or {@link AbstractJdbiConfigurationModule#configureJdbi()}.
     */
    default LinkedBindingBuilder<Codec<?>> bindCodec(QualifiedType<?> qualifiedType) {
        return jdbiBinder().bindCodec(qualifiedType);
    }

    /**
     * Creates a new binding for a {@link Codec} using a {@link GenericType}.
     * <p>
     * Only valid when called from {@link AbstractJdbiDefinitionModule#configureJdbi()} or {@link AbstractJdbiConfigurationModule#configureJdbi()}.
     */
    default LinkedBindingBuilder<Codec<?>> bindCodec(GenericType<?> genericType) {
        return jdbiBinder().bindCodec(genericType);
    }

    /**
     * Creates a new binding for a {@link Codec} using a {@link Type}.
     * <p>
     * Only valid when called from {@link AbstractJdbiDefinitionModule#configureJdbi()} or {@link AbstractJdbiConfigurationModule#configureJdbi()}.
     */
    default LinkedBindingBuilder<Codec<?>> bindCodec(Type type) {
        return jdbiBinder().bindCodec(type);
    }

    /**
     * Creates a new binding for a SQL array type.
     * <p>
     * Only valid when called from {@link AbstractJdbiDefinitionModule#configureJdbi()} or {@link AbstractJdbiConfigurationModule#configureJdbi()}.
     */
    default LinkedBindingBuilder<String> bindArrayType(Class<?> arrayType) {
        return jdbiBinder().bindArrayType(arrayType);
    }

    /**
     * Creates a new binding for a {@link JdbiPlugin}.
     * <p>
     * Only valid when called from {@link AbstractJdbiDefinitionModule#configureJdbi()} or {@link AbstractJdbiConfigurationModule#configureJdbi()}.
     */
    default LinkedBindingBuilder<JdbiPlugin> bindPlugin() {
        return jdbiBinder().bindPlugin();
    }

    /**
     * Creates a new binding for a {@link GuiceJdbiCustomizer}. All registered customizers are called on a newly created {@link org.jdbi.v3.core.Jdbi} object
     * and allow further customization of all aspects of the {@link org.jdbi.v3.core.Jdbi} object.
     * <p>
     * Only valid when called from {@link AbstractJdbiDefinitionModule#configureJdbi()} or {@link AbstractJdbiConfigurationModule#configureJdbi()}.
     */
    default LinkedBindingBuilder<GuiceJdbiCustomizer> bindCustomizer() {
        return jdbiBinder().bindCustomizer();
    }
}
