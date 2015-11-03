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

import org.jdbi.v3.StatementContext;
import org.jdbi.v3.tweak.ResultColumnMapper;
import org.jdbi.v3.tweak.ResultSetMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Entity;
import java.lang.reflect.Constructor;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AnnoMapper<C> implements ResultSetMapper<C> {

    private final Class<C> clazz;
    private final AnnoClass<C> annos;

    public static boolean accept(Class<?> clazz) {
        if (logger.isDebugEnabled()) {
            logger.debug("accept " + clazz);
        }
        return clazz.getAnnotation(Entity.class) != null;
    }

    public static <C> AnnoMapper<C> get(Class<C> clazz) {
        return new AnnoMapper<C>(clazz);
    }

    private AnnoMapper(Class<C> clazz) {
        if (logger.isDebugEnabled()) {
            logger.debug("init " + clazz);
        }
        this.clazz = clazz;
        this.annos = AnnoClass.get(clazz);
    }

    @Override
    public C map(int i, ResultSet rs, StatementContext ctx) throws SQLException {
        C obj;
        if (logger.isDebugEnabled()) {
            logger.debug("map " + clazz);
        }
        try {
            Constructor<C> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            obj = constructor.newInstance();
            for (AnnoMember annoMember : annos.setters()) {
                annoMember.write(obj, get(annoMember, rs, ctx));
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return obj;
    }

    private Object get(AnnoMember annoMember, ResultSet rs, StatementContext ctx) throws SQLException {
        Class memberType = annoMember.getType();
        String name = annoMember.getName();
        ResultColumnMapper<?> columnMapper = ctx.columnMapperFor(memberType);
        return columnMapper.mapColumn(rs, name, ctx);
    }

    private static Logger logger = LoggerFactory.getLogger(AnnoMapper.class);
}
