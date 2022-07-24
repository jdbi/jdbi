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
package org.jdbi.v3.postgres.postgis;

import java.util.function.Function;

import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.codec.Codec;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.meta.Alpha;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;

import static org.locationtech.jts.io.WKBConstants.wkbNDR;

@Alpha
final class PostgisCodec implements Codec<Geometry> {

    @Override
    public ColumnMapper<Geometry> getColumnMapper() {
        return (resultSet, index, context) -> {
            byte[] bytes = hexStringToByteArray(resultSet.getString(index));
            return (Geometry) deserialize(bytes);
        };
    }

    @Override
    public Function<Geometry, Argument> getArgumentFunction() {
        return data -> (position, statement, context) -> statement.setBytes(position, serialize(data));
    }

    /**
     * Serializes a geometry in the WKB format.
     *
     * @param geometry
     * @return
     */
    private static byte[] serialize(Geometry geometry) {
        if (geometry == null) {
            return null;
        }
        WKBWriter writer = new WKBWriter(2, wkbNDR, true);
        return writer.write(geometry);
    }

    /**
     * Deserializes a geometry in the WKB format.
     *
     * @param wkb
     * @return
     */
    private static Geometry deserialize(byte[] wkb) {
        if (wkb == null) {
            return null;
        }
        try {
            WKBReader reader = new WKBReader(new GeometryFactory());
            return reader.read(wkb);
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] =
                (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
