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

package org.jdbi.postgis;

import org.jdbi.core.Jdbi;
import org.jdbi.core.codec.Codec;
import org.jdbi.core.codec.CodecFactory;
import org.jdbi.core.spi.JdbiPlugin;
import org.jdbi.meta.Beta;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

/**
 * Postgis plugin. Adds support for binding and mapping the following data types:
 *
 * <ul>
 * <li>{@link org.locationtech.jts.geom.Point}</li>
 * <li>{@link org.locationtech.jts.geom.LineString}</li>
 * <li>{@link org.locationtech.jts.geom.LinearRing}</li>
 * <li>{@link org.locationtech.jts.geom.Polygon}</li>
 * <li>{@link org.locationtech.jts.geom.MultiPoint}</li>
 * <li>{@link org.locationtech.jts.geom.MultiLineString}</li>
 * <li>{@link org.locationtech.jts.geom.MultiPolygon}</li>
 * <li>{@link org.locationtech.jts.geom.GeometryCollection}</li>
 * <li>{@link org.locationtech.jts.geom.Geometry}</li>
 * </ul>
 */
@Beta
public class PostgisPlugin extends JdbiPlugin.Singleton {

    @Override
    public void customizeJdbi(Jdbi jdbi) {
        final Codec<Geometry> codec = new PostgisCodec();

        jdbi.registerCodecFactory(CodecFactory.builder()
            .addCodec(Geometry.class, codec)
            .addCodec(GeometryCollection.class, codec)
            .addCodec(LinearRing.class, codec)
            .addCodec(LineString.class, codec)
            .addCodec(MultiLineString.class, codec)
            .addCodec(MultiPoint.class, codec)
            .addCodec(MultiPolygon.class, codec)
            .addCodec(Point.class, codec)
            .addCodec(Polygon.class, codec)
            .build());
    }
}
