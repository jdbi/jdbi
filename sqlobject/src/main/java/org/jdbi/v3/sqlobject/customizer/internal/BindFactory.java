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
package org.jdbi.v3.sqlobject.customizer.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Optional;

import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizerFactory;
import org.jdbi.v3.sqlobject.customizer.SqlStatementParameterCustomizer;
import org.jdbi.v3.sqlobject.internal.ParameterUtil;

import static org.jdbi.v3.core.qualifier.Qualifiers.getQualifiers;

public class BindFactory implements SqlStatementCustomizerFactory {
    @Override
    public SqlStatementParameterCustomizer createForParameter(Annotation annotation,
                                                              Class<?> sqlObjectType,
                                                              Method method,
                                                              Parameter param,
                                                              int index,
                                                              Type type) {
        Bind b = (Bind) annotation;
        String nameFromAnnotation = b == null ? Bind.NO_VALUE : b.value();
        Optional<String> name = ParameterUtil.findParameterName(nameFromAnnotation, param);
        QualifiedType<?> qualifiedType = QualifiedType.of(type).withAnnotations(getQualifiers(param));

        return (stmt, arg) -> {
            stmt.bindByType(index, arg, qualifiedType);
            name.ifPresent(n -> stmt.bindByType(n, arg, qualifiedType));
        };
    }
}
