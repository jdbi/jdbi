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
package org.jdbi.v3.sqlobject.statement.internal;

import java.lang.reflect.Method;
import java.util.function.Function;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.generic.GenericTypes;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.qualifier.Qualifiers;
import org.jdbi.v3.core.result.ResultBearing;
import org.jdbi.v3.core.result.ResultIterable;
import org.jdbi.v3.core.statement.Update;
import org.jdbi.v3.sqlobject.UnableToCreateSqlObjectException;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;
import org.jdbi.v3.sqlobject.statement.UseRowReducer;

import static java.lang.String.format;

public class SqlUpdateHandler extends CustomizingStatementHandler<Update> {

    private final Function<Update, Object> resultTransformer;
    private final ResultReturner resultReturner;

    public SqlUpdateHandler(Class<?> sqlObjectType, Method method) {
        super(sqlObjectType, method);

        if (method.isAnnotationPresent(UseRowReducer.class)) {
            throw new UnsupportedOperationException("Cannot declare @UseRowReducer on a @SqlUpdate method.");
        }

        GetGeneratedKeys getGeneratedKeys = method.getAnnotation(GetGeneratedKeys.class);

        QualifiedType<?> returnType = QualifiedType.of(
                        GenericTypes.resolveType(method.getGenericReturnType(), sqlObjectType))
                .withAnnotations(new Qualifiers().findFor(method));

        if (getGeneratedKeys != null) {
            this.resultReturner = ResultReturner.forMethod(sqlObjectType, method);

            String[] columnNames = getGeneratedKeys.value();

            this.resultTransformer = update -> {
                ResultBearing resultBearing = update.executeAndReturnGeneratedKeys(columnNames);

                UseRowMapper useRowMapper = method.getAnnotation(UseRowMapper.class);
                ResultIterable<?> iterable = useRowMapper == null
                        ? resultBearing.mapTo(returnType)
                        : resultBearing.map(rowMapperFor(useRowMapper));

                return resultReturner.mappedResult(iterable, update.getContext());
            };
        } else if (isLong(method.getReturnType())) {
            this.resultTransformer = Update::executeLarge;
            this.resultReturner = null;
        } else if (isNumeric(method.getReturnType())) {
            this.resultTransformer = Update::execute;
            this.resultReturner = null;
        } else if (isBoolean(method.getReturnType())) {
            this.resultTransformer = update -> update.execute() > 0;
            this.resultReturner = null;
        } else {
            throw new UnableToCreateSqlObjectException(format(
                    "%s.%s method is annotated with @SqlUpdate so should return void, boolean, int, or long but is returning: %s",
                    method.getDeclaringClass().getSimpleName(), method.getName(), returnType));
        }
    }

    @Override
    public void warm(ConfigRegistry config) {
        super.warm(config);
        if (resultReturner != null) {
            resultReturner.warm(config);
        }
    }

    @Override
    Update createStatement(Handle handle, String locatedSql) {
        return handle.createUpdate(locatedSql);
    }

    @Override
    void configureReturner(Update u, SqlObjectStatementConfiguration cfg) {
        cfg.setReturner(() -> resultTransformer.apply(u));
    }

    private boolean isNumeric(Class<?> type) {
        return type.equals(Integer.class)
                || type.equals(int.class)
                || type.equals(void.class)
                || type.equals(Void.class);
    }

    private boolean isBoolean(Class<?> type) {
        return type.equals(boolean.class) || type.equals(Boolean.class);
    }

    private boolean isLong(Class<?> type) {
        return type.equals(long.class) || type.equals(Long.class);
    }
}
