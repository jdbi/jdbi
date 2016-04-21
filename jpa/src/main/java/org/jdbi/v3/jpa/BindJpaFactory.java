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

import org.jdbi.v3.sqlobject.Binder;
import org.jdbi.v3.sqlobject.BinderFactory;

import java.lang.reflect.InvocationTargetException;

public class BindJpaFactory implements BinderFactory<BindJpa, Object> {
    @Override
    public Binder<BindJpa, Object> build(BindJpa annotation) {
        return (q, parameter, index, bind, arg) -> {
            final String prefix;
            if (BindJpa.DEFAULT.equals(bind.value())) {
                prefix = "";
            } else {
                prefix = bind.value() + ".";
            }

            JpaClass<?> jpaClass = JpaClass.get(arg.getClass());
            for (JpaMember member : jpaClass.members()) {
                q.bindByType(
                        prefix + member.getColumnName(),
                        readMember(arg, member),
                        member.getType());
            }
        };
    }

    private static Object readMember(Object entity, JpaMember member) {
        try {
            return member.read(entity);
        } catch (IllegalAccessException e) {
            String message = String.format(
                    "Unable to access property value for column %s",
                    member.getColumnName());
            throw new EntityMemberAccessException(message, e);
        } catch (InvocationTargetException e) {
            String message = String.format(
                    "Exception thrown in accessor method for column %s",
                    member.getColumnName());
            throw new EntityMemberAccessException(message, e);
        }
    }
}
