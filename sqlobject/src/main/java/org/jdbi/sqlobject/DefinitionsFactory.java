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
package org.jdbi.sqlobject;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jdbi.core.extension.ExtensionMetadata;
import org.jdbi.core.statement.SqlStatements;
import org.jdbi.sqlobject.customizer.Definition;

class DefinitionsFactory {
    private final Map<String, Object> definitions = new HashMap<>();

    private DefinitionsFactory() {}

    static void configureDefinitions(Class<?> extensionType, ExtensionMetadata.Builder builder) {
        new DefinitionsFactory().configure(extensionType, builder);
    }

    void configure(Class<?> extensionType, ExtensionMetadata.Builder builder) {
        configureTypeDefinitions(extensionType);

        if (!definitions.isEmpty()) {
            builder.addInstanceConfigCustomizer(config ->
                    config.get(SqlStatements.class)
                            .defineMap(definitions));
        }
    }

    private void configureTypeDefinitions(Class<?> type) {
        Arrays.asList(type.getInterfaces())
                .forEach(this::configureTypeDefinitions);
        Optional.ofNullable(type.getSuperclass())
                .ifPresent(this::configureTypeDefinitions);
        for (var typeDefinition : type.getAnnotationsByType(Definition.class)) {
            if (notDefined(typeDefinition.key())) {
                throw new UnableToCreateSqlObjectException(String.format(
                        "Type level @Definition on %s must have specific key",
                        type));
            }

            if (notDefined(typeDefinition.value())) {
                throw new UnableToCreateSqlObjectException(String.format(
                        "Type level @Definition on %s must have specific value",
                        type));
            }

            definitions.put(typeDefinition.key(), typeDefinition.value());
        }
        configureFieldDefinitions(type);
        configureMethodDefinitions(type);
    }

    private void configureFieldDefinitions(Class<?> type) {
        for (var field : type.getDeclaredFields()) {
            for (var fieldDefinition : field.getAnnotationsByType(Definition.class)) {
                if (defined(fieldDefinition.value())) {
                    throw new UnableToCreateSqlObjectException(String.format(
                            "Field %s @Definition on %s may not specify value",
                            field.getName(), type));
                }

                try {
                    definitions.put(
                            defaultValue(fieldDefinition.key(), field.getName()),
                            field.get(null));
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    throw new UnableToCreateSqlObjectException(
                            String.format("Could not read field %s of %s", field.getName(), type),
                            e);
                }
            }
        }
    }

    private void configureMethodDefinitions(Class<?> type) {
        for (var method : type.getDeclaredMethods()) {
            for (var methodDefinition : method.getAnnotationsByType(Definition.class)) {
                if (method.getParameterCount() > 0) {
                    throw new UnableToCreateSqlObjectException(
                            String.format("@Definition annotated method %s may not have any parameters (on %s)",
                                    method.getName(), type));
                }

                if (defined(methodDefinition.value())) {
                    throw new UnableToCreateSqlObjectException(String.format(
                            "Method %s @Definition on %s may not specify value",
                            method.getName(), type));
                }

                try {
                    definitions.put(
                            defaultValue(methodDefinition.key(), method.getName()),
                            method.invoke(null));
                } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
                    throw new UnableToCreateSqlObjectException(
                            String.format("Could not invoke method %s of %s", method.getName(), type),
                            e);
                }
            }
        }
    }

    private static boolean defined(String v) {
        return !notDefined(v);
    }

    private static boolean notDefined(String v) {
        return Definition.UNDEFINED.equals(v);
    }

    private static String defaultValue(String value, String dfl) {
        if (notDefined(value)) {
            return dfl;
        }
        return value;
    }
}
