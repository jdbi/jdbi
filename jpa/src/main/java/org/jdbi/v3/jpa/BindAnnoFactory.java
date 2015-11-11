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

public class BindAnnoFactory implements BinderFactory<BindAnno, Object> {
    @Override
    public Binder<BindAnno, Object> build(BindAnno annotation) {
        return (q, parameter, bind, arg) -> {
            final String prefix;
            if (BindAnno.DEFAULT.equals(bind.value())) {
                prefix = "";
            } else {
                prefix = bind.value() + ".";
            }

            try {
                AnnoClass<?> annoClass = AnnoClass.get(arg.getClass());
                for (AnnoMember member : annoClass.members()) {
                    q.dynamicBind(member.getType(), prefix + member.getColumnName(), member.read(arg));
                }
            } catch (Exception e) {
                throw new IllegalStateException(
                        "unable to bind bean properties", e);
            }
        };
    }
}
