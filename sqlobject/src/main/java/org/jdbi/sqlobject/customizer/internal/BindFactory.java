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
package org.jdbi.sqlobject.customizer.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.Optional;

import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.mapper.Mappers;
import org.jdbi.core.qualifier.QualifiedType;
import org.jdbi.core.qualifier.Qualifiers;
import org.jdbi.core.statement.SqlStatement;
import org.jdbi.sqlobject.customizer.Bind;
import org.jdbi.sqlobject.customizer.SqlStatementCustomizerFactory;
import org.jdbi.sqlobject.customizer.SqlStatementParameterCustomizer;
import org.jdbi.sqlobject.internal.ParameterUtil;

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

        return new SqlStatementParameterCustomizer() {
            @Override
            public void apply(SqlStatement<?> stmt, Object arg) throws SQLException {
                QualifiedType<?> qualifiedType = qualifiedType(stmt.getConfig());
                stmt.bindByType(index, arg, qualifiedType);
                name.ifPresent(n -> stmt.bindByType(n, arg, qualifiedType));
            }

            @Override
            public void warm(ConfigRegistry config) {
                config.get(Mappers.class).findFor(qualifiedType(config));
            }

            private QualifiedType<?> qualifiedType(ConfigRegistry config) {
                return QualifiedType.of(type).withAnnotations(
                        config.get(Qualifiers.class).findFor(param));
            }
        };
    }
}
