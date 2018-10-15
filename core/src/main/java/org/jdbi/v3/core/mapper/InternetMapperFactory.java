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
package org.jdbi.v3.core.mapper;

import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.statement.StatementContext;

import static org.jdbi.v3.core.generic.GenericTypes.getErasedType;

/**
 * Column mapper factory which knows how to map JDBC-recognized types, along with some other well-known types
 * from the JDK.
 */
public class InternetMapperFactory implements ColumnMapperFactory {
    private static final Map<Class<?>, ColumnMapper<?>> MAPPERS = new HashMap<>();

    static {
        MAPPERS.put(InetAddress.class, InternetMapperFactory::getInetAddress);
        MAPPERS.put(URL.class, referenceMapper(ResultSet::getURL));
        MAPPERS.put(URI.class, referenceMapper(InternetMapperFactory::getURI));
    }

    @Override
    public Optional<ColumnMapper<?>> build(Type type, ConfigRegistry config) {
        Class<?> rawType = getErasedType(type);

        return Optional.ofNullable(MAPPERS.get(rawType));
    }

    private static <T> ColumnMapper<T> referenceMapper(ColumnGetter<T> getter) {
        return (r, i, ctx) -> {
            T value = getter.get(r, i);
            return r.wasNull() ? null : value;
        };
    }

    private static URI getURI(ResultSet r, int i) throws SQLException {
        String s = r.getString(i);
        try {
            return s == null ? null : new URI(s);
        } catch (URISyntaxException e) {
            throw new SQLException("Failed to convert data to URI", e);
        }
    }

    private static InetAddress getInetAddress(ResultSet r, int i, StatementContext ctx) throws SQLException {
        String hostname = r.getString(i);
        try {
            return hostname == null ? null : InetAddress.getByName(hostname);
        } catch (UnknownHostException e) {
            throw new MappingException("Could not map InetAddress", e);
        }
    }
}
