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

import org.jdbi.v3.core.StatementContext;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.RowMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JpaMapper<C> implements RowMapper<C> {

    private final Class<C> clazz;
    private final JpaClass<C> jpaClass;

    JpaMapper(Class<C> clazz) {
        logger.debug("init {}", clazz);
        this.clazz = clazz;
        this.jpaClass = JpaClass.get(clazz);
    }

    @Override
    public C map(ResultSet rs, StatementContext ctx) throws SQLException {
        logger.debug("map {}", clazz);
        try {
            return tryMap(rs, ctx);
        } catch (NoSuchMethodException |
                InstantiationException |
                IllegalAccessException |
                InvocationTargetException e) {
            throw new EntityMemberAccessException(String.format("Unable to map %s entity", clazz), e);
        }
    }

    private C tryMap(ResultSet rs, StatementContext ctx)
            throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, SQLException {
        Constructor<C> constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        C obj = constructor.newInstance();
        for (int colIndex = rs.getMetaData().getColumnCount(); colIndex >= 1; colIndex--) {
            String columnLabel = rs.getMetaData().getColumnLabel(colIndex);
            JpaMember member = jpaClass.lookupMember(columnLabel);
            if (member != null) {
                Type memberType = member.getType();
                ColumnMapper<?> columnMapper = ctx.findColumnMapperFor(memberType)
                        .orElseThrow(() -> new NoSuchColumnMapperException("No column mapper for " + memberType));
                Object value = columnMapper.map(rs, columnLabel, ctx);
                member.write(obj, value);
            }
        }
        return obj;
    }

    private static final Logger logger = LoggerFactory.getLogger(JpaMapper.class);
}
