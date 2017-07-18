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

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.NoSuchMapperException;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.jpa.internal.JpaClass;
import org.jdbi.v3.jpa.internal.JpaMember;

/**
 * Row mapper for a JPA-annotated type as a result.
 * @param <C> the type to map
 */
public class JpaMapper<C> implements RowMapper<C> {

    private final Class<C> clazz;
    private final JpaClass<C> jpaClass;

    JpaMapper(Class<C> clazz) {
        this.clazz = clazz;
        this.jpaClass = JpaClass.get(clazz);
    }

    @Override
    public RowMapper<C> specialize(ResultSet rs, StatementContext ctx) throws SQLException
    {
        Constructor<C> constructor;
        try {
            constructor = clazz.getDeclaredConstructor();
        } catch (ReflectiveOperationException e) {
            throw new EntityMemberAccessException("Unable to get constructor for " + clazz, e);
        }
        constructor.setAccessible(true);

        List<MemberSetter<C>> setters = new ArrayList<>();

        for (int colIndex = rs.getMetaData().getColumnCount(); colIndex >= 1; colIndex--) {
            String columnLabel = rs.getMetaData().getColumnLabel(colIndex);
            JpaMember member = jpaClass.lookupMember(columnLabel);
            if (member != null) {
                Type memberType = member.getType();
                ColumnMapper<?> columnMapper = ctx.findColumnMapperFor(memberType)
                        .orElseThrow(() -> new NoSuchMapperException("No column mapper for " + memberType));

                final int columnIndex = colIndex;
                setters.add(obj -> member.write(obj, columnMapper.map(rs, columnIndex, ctx)));
            }
        }

        return (r, c) -> {
            C obj;
            try {
                obj = constructor.newInstance();
            } catch (ReflectiveOperationException e) {
                throw new EntityMemberAccessException("Unable to invoke " + constructor, e);
            }
            for (MemberSetter<C> setter : setters) {
                setter.mapAndSetMember(obj);
            }
            return obj;
        };
    }

    @Override
    public C map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        return specialize(rs, ctx).map(rs, ctx);
    }

    @FunctionalInterface
    private interface MemberSetter<C> {
        void mapAndSetMember(C object) throws SQLException;
    }
}
