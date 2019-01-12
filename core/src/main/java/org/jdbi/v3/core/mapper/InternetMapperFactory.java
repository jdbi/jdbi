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
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.statement.StatementContext;

import static org.jdbi.v3.core.generic.GenericTypes.getErasedType;

/**
 * Column mapper factory which knows how to map networking related objects:
 * <ul>
 *     <li>{@link InetAddress}</li>
 *     <li>{@link URL}</li>
 *     <li>{@link URI}</li>
 * </ul>
 */
class InternetMapperFactory implements ColumnMapperFactory {
    private final Map<Class<?>, ColumnMapper<?>> mappers = new IdentityHashMap<>();

    InternetMapperFactory() {
        mappers.put(InetAddress.class, InternetMapperFactory::getInetAddress);
        mappers.put(URL.class, new GetterMapper<>(ResultSet::getURL));
        mappers.put(URI.class, new GetterMapper<>(InternetMapperFactory::getURI));
    }

    @Override
    public Optional<ColumnMapper<?>> build(Type type, ConfigRegistry config) {
        Class<?> rawType = getErasedType(type);

        return Optional.ofNullable(mappers.get(rawType));
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
