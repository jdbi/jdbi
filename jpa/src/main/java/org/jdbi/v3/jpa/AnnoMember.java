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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Column;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class AnnoMember {

    private Column column;
    private String name;
    private Field field;
    private Method method;
    private Class<?> clazz;
    private Class<?> type;

    AnnoMember(Class<?> clazz, Field member) {
        this.clazz = clazz;
        this.field = member;
        this.column = member.getAnnotation(Column.class);
        this.name = nameOf(member, column);
        this.type = member.getType();
    }

    AnnoMember(Class<?> clazz, Method member) {
        this.clazz = clazz;
        this.method = member;
        this.column = member.getAnnotation(Column.class);
        this.name = nameOf(member, column);
        this.type = member.getParameterTypes()[0];
    }

    public String getName() {
        return name;
    }

    public Class<?> getType() {
        return type;
    }

    public Object read(Object obj) throws IllegalArgumentException,
            IllegalAccessException, InvocationTargetException {
        if (method != null) {
            method.setAccessible(true);
            return method.invoke(obj);
        }
        if (field != null) {
            field.setAccessible(true);
            return field.get(obj);
        }
        // unreachable!
        throw new RuntimeException("Reached unreachable!");
    }

    public void write(Object obj, Object value)
            throws IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {
        if (logger.isDebugEnabled()) {
            logger.debug("write" + clazz + "/" + name + "/" + type + "/" + value);
        }
        if (method != null) {
            method.setAccessible(true);
            method.invoke(obj, value);
        }
        if (field != null) {
            field.setAccessible(true);
            field.set(obj, value);
        }
    }

    private String nameOf(Field member, Column column) {
        String name = column.name();
        if (name == null || name.length() == 0) {
            name = member.getName();
        }
        return name;
    }

    private String nameOf(Method member, Column column) {
        String name = column.name();
        if (name == null || name.length() == 0) {
            name = member.getName();
            // TODO: drop set/get/is
        }
        return name;
    }

    private static Logger logger = LoggerFactory.getLogger(AnnoMember.class);
}
