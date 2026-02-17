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
package org.jdbi.sqlobject.statement.internal;

import java.lang.reflect.Method;
import java.util.function.Function;

import org.jdbi.core.Handle;
import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.generic.GenericTypes;
import org.jdbi.core.qualifier.QualifiedType;
import org.jdbi.core.qualifier.Qualifiers;
import org.jdbi.core.result.ResultBearing;
import org.jdbi.core.result.ResultIterable;
import org.jdbi.core.statement.Update;
import org.jdbi.sqlobject.UnableToCreateSqlObjectException;
import org.jdbi.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.sqlobject.statement.UseRowMapper;
import org.jdbi.sqlobject.statement.UseRowReducer;

import static java.lang.String.format;

public class SqlUpdateHandler extends CustomizingStatementHandler<Update> {

    private final WarmableResultTransformer resultTransformer;

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
            String[] columnNames = getGeneratedKeys.value();
            var resultReturner = ResultReturner.forMethod(sqlObjectType, method);

            this.resultTransformer = new WarmableResultTransformer() {
                @Override
                public Object apply(Update update) {
                    var ctx = update.getContext();
                    var elementType = resultReturner.elementType(ctx.getConfig());
                    ResultBearing resultBearing = update.executeAndReturnGeneratedKeys(columnNames);

                    UseRowMapper useRowMapper = method.getAnnotation(UseRowMapper.class);
                    ResultIterable<?> iterable = useRowMapper == null
                        ? resultBearing.mapTo(elementType)
                        : resultBearing.map(rowMapperFor(useRowMapper));

                    return resultReturner.mappedResult(iterable, update.getContext());
                }

                @Override
                public void warm(ConfigRegistry config) {
                    resultReturner.warm(config);
                }
            };
        } else if (isLong(method.getReturnType())) {
            this.resultTransformer = Update::executeLarge;
        } else if (isNumeric(method.getReturnType())) {
            this.resultTransformer = Update::execute;
        } else if (isBoolean(method.getReturnType())) {
            this.resultTransformer = update -> update.execute() > 0;
        } else {
            throw new UnableToCreateSqlObjectException(format(
                    "%s.%s method is annotated with @SqlUpdate and should return void, boolean, int, long, or have a @GetGeneratedKeys annotation, but is returning: %s",
                    method.getDeclaringClass().getSimpleName(), method.getName(), returnType));
        }
    }

    @Override
    protected void warm(ConfigRegistry config) {
        this.resultTransformer.warm(config);
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

    private interface WarmableResultTransformer extends Function<Update, Object> {
        default void warm(ConfigRegistry config) {}
    }
}
