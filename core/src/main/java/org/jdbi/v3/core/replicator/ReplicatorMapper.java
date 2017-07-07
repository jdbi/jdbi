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
package org.jdbi.v3.core.replicator;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.jdbi.v3.core.generic.GenericTypes;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.reflect.ReflectionMappers;
import org.jdbi.v3.core.result.UnableToProduceResultException;
import org.jdbi.v3.core.statement.StatementContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ReplicatorMapper implements RowMapper<Object> {
    private static final Logger LOG = LoggerFactory.getLogger(ReplicatorMapper.class);
    private static final Method TO_STRING, HASH_CODE, EQUALS, INTERNAL_GET;
    private final Type type;

    static {
        try {
            TO_STRING = Object.class.getMethod("toString");
            HASH_CODE = Object.class.getMethod("hashCode");
            EQUALS = Object.class.getMethod("equals", Object.class);
            INTERNAL_GET = ResultMap.class.getMethod("internalGetResultMap");
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    ReplicatorMapper(Type type) {
        this.type = type;
    }

    @Override
    public Object map(ResultSet rs, StatementContext ctx) throws SQLException {
        return specialize(rs, ctx).map(rs, ctx);
    }

    @Override
    public RowMapper<Object> specialize(ResultSet rs, StatementContext ctx) throws SQLException {
        final Class<?> resultClass = GenericTypes.getErasedType(type);
        final ResultSetMetaData md = rs.getMetaData();
        final BeanInfo info;
        try {
            info = Introspector.getBeanInfo(resultClass);
        } catch (IntrospectionException e) {
            throw new UnableToProduceResultException("while inspecting " + type, e, ctx);
        }
        final ReflectionMappers rm = ctx.getConfig().get(ReflectionMappers.class);
        final Map<Method, Getter> getters = new HashMap<>();

        for (PropertyDescriptor p : info.getPropertyDescriptors()) {
            Getter getter = null;
            // Try to match a column and ColumnMapper to provide
            for (int i = 1; i <= md.getColumnCount(); i++) {
                final String label = md.getColumnLabel(i);
                final String name = rm.paramName(p);
                LOG.trace("Matching property '{}' against column '{}'", name, label);
                if (rm.columnNameMatches(label, name)) {
                    if (getter != null) {
                        throw new UnableToProduceResultException("Duplicate column provider found for " + p, ctx);
                    }
                    final ColumnMapper<?> m = ctx.findColumnMapperFor(p.getReadMethod().getGenericReturnType())
                        .orElseThrow(() ->
                            new UnableToProduceResultException("No column mapper found for type " + p.getPropertyType() + " of property " + p, ctx));
                    final int idx = i;
                    LOG.trace("Found column #{} '{}' for method '{}'", idx, label, name);
                    getter = r -> m.map(r, idx, ctx);
                }
            }
            if (getter == null) {
                throw new UnableToProduceResultException("No column matches " + p.getName(), ctx);
            }
            getters.put(p.getReadMethod(), getter);
        }

        final ClassLoader loader = resultClass.getClassLoader();
        return (rs1, ctx1) -> Proxy.newProxyInstance(loader, new Class<?>[] { resultClass, ResultMap.class }, buildHandler(getters, rs1, ctx1));
    }

    private InvocationHandler buildHandler(Map<Method, Getter> getters, ResultSet rs, StatementContext ctx) {
        final Map<Method, Object> results = new HashMap<>();
        getters.forEach((m, g) -> results.put(m, get(g, rs, ctx)));
        results.put(HASH_CODE, results.hashCode());
        results.put(TO_STRING, results.toString());
        return (proxy, method, args) -> {
            if (method.equals(INTERNAL_GET)) {
                return results;
            }
            if (method.equals(EQUALS)) {
                if (args[0] == null || args[0].getClass() != proxy.getClass()) {
                    return false;
                }
                return results.equals(((ResultMap) args[0]).internalGetResultMap());
            }
            return results.get(method);
        };
    }

    private Object get(Getter g, ResultSet rs, StatementContext ctx) {
        try {
            return g.get(rs);
        } catch (SQLException e) {
            throw new UnableToProduceResultException("Unable to get value", e, ctx);
        }
    }

    @FunctionalInterface
    interface Getter {
        Object get(ResultSet rs) throws SQLException;
    }

    interface ResultMap {
        Map<Method, Object> internalGetResultMap();
    }
}
