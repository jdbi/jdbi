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
import javax.persistence.MappedSuperclass;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class AnnoClass<C> {

    private static final Map<Class<?>, AnnoClass<?>> cache = new ConcurrentHashMap<>();

    public static <C> AnnoClass<C> get(Class<C> clazz) {
        return (AnnoClass<C>) cache.computeIfAbsent(clazz, AnnoClass::new);
    }

    private final List<AnnoMember> setters = new ArrayList<>();
    private final List<AnnoMember> getters = new ArrayList<>();

    private AnnoClass(Class<C> clazz) {
        if (logger.isDebugEnabled()) {
            logger.debug("init " + clazz);
        }
        inspectClass(clazz);
        inspectSuperclasses(clazz);
        if (logger.isDebugEnabled()) {
            logger.debug("init " + clazz + ": " + setters.size() + " setters and " + getters.size() + " getters.");
        }
    }

    private void inspectSuperclasses(Class<? super C> clazz) {
        while ((clazz = clazz.getSuperclass()) != null) {
            if (clazz.isAnnotationPresent(MappedSuperclass.class)) {
                inspectClass(clazz);
            }
        }
    }

    private void inspectClass(Class<? super C> clazz) {
        for (Field member : clazz.getDeclaredFields()) {
            if (member.getAnnotation(Column.class) != null) {
                setters.add(new AnnoMember(clazz, member));
                getters.add(new AnnoMember(clazz, member));
            }
        }
        for (Method member : clazz.getDeclaredMethods()) {
            if (member.getAnnotation(Column.class) == null) {
                continue;
            }
            if (member.getParameterTypes().length == 1) {
                setters.add(new AnnoMember(clazz, member));
            } else if (member.getParameterTypes().length == 0) {
                getters.add(new AnnoMember(clazz, member));
            }
        }
    }

    public List<AnnoMember> setters() {
        return setters;
    }

    public List<AnnoMember> getters() {
        return getters;
    }

    private static Logger logger = LoggerFactory.getLogger(AnnoClass.class);
}
