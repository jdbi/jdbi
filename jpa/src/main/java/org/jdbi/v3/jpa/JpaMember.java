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
package org.jdbi.v3.jpa;

import static java.util.Objects.requireNonNull;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Optional;

import javax.persistence.Column;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class JpaMember {

    interface Getter {
        Object get(Object obj) throws IllegalAccessException, InvocationTargetException;
    }

    interface Setter {
        void set(Object obj, Object value) throws IllegalAccessException, InvocationTargetException;
    }

    private final Class<?> clazz;
    private final String columnName;
    private final Type type;
    private final Getter accessor;
    private final Setter mutator;

    JpaMember(Class<?> clazz, Column column, Field field) {
        this.clazz = requireNonNull(clazz);
        this.columnName = nameOf(column, field.getName());
        this.type = field.getGenericType();
        field.setAccessible(true);
        this.accessor = field::get;
        this.mutator = field::set;
    }

    JpaMember(Class<?> clazz, Column column, PropertyDescriptor property) {
        this.clazz = requireNonNull(clazz);
        this.columnName = nameOf(column, property.getName());
        this.type = property.getReadMethod().getGenericReturnType();

        Method getter = property.getReadMethod();
        Method setter = property.getWriteMethod();
        getter.setAccessible(true);
        setter.setAccessible(true);
        this.accessor = getter::invoke;
        this.mutator = setter::invoke;
    }

    public String getColumnName() {
        return columnName;
    }

    public Type getType() {
        return type;
    }

    public Object read(Object obj) throws IllegalAccessException, InvocationTargetException {
        return accessor.get(obj);
    }

    public void write(Object obj, Object value) {
        logger.debug("write {}/{}/{}/{}", clazz, columnName, type, value);

        try {
            mutator.set(obj, value);
        } catch (ReflectiveOperationException e) {
            throw new EntityMemberAccessException("Couldn't set " + clazz + "#" + columnName, e);
        }
    }

    private static String nameOf(Column column, String memberName) {
        return Optional.ofNullable(column)
                .map(Column::name)
                .filter(name -> name.length() > 0)
                .orElse(memberName);
    }

    private static final Logger logger = LoggerFactory.getLogger(JpaMember.class);
}
