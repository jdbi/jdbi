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

import org.jdbi.core.Handle;
import org.jdbi.postgres.PostgresPlugin;
import org.jdbi.sqlobject.SqlObjectPlugin;
import org.jdbi.testing.junit5.JdbiExtension;
import org.jdbi.testing.junit5.tc.JdbiTestcontainersExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("slow")
@Testcontainers
final class PostgisPluginTest {

    @Container
    public static JdbcDatabaseContainer<?> pgContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgis/postgis:13-3.3-alpine")
        .asCompatibleSubstituteFor("postgres"));

    @RegisterExtension
    public JdbiExtension pgExtension = JdbiTestcontainersExtension.instance(pgContainer)
        .withPlugins(new SqlObjectPlugin(), new PostgresPlugin(), new PostgisPlugin())
        .withInitializer((ds, h) -> h.execute(
            "CREATE TABLE record (id INTEGER PRIMARY KEY, point public.geometry(point), linestring public.geometry(linestring), polygon public.geometry(polygon))"));


    private Handle handle;

    @BeforeEach
    public void before() {
        this.handle = pgExtension.openHandle();
    }

    @AfterEach
    public void after() {
        this.handle.close();
    }

    @Test
    void postgisSmokeTest() {
        PostgisRecord record = new PostgisRecord();
        record.setId(1);
        record.setPoint(new GeometryFactory().createPoint(new Coordinate(1, 1)));
        record.setLineString(
            new GeometryFactory()
                .createLineString(
                    new Coordinate[]{
                        new Coordinate(1, 1),
                        new Coordinate(1, 2),
                        new Coordinate(2, 2),
                        new Coordinate(2, 1)
                    }));
        record.setPolygon(
            new GeometryFactory()
                .createPolygon(
                    new Coordinate[]{
                        new Coordinate(1, 1),
                        new Coordinate(1, 2),
                        new Coordinate(2, 2),
                        new Coordinate(2, 1),
                        new Coordinate(1, 1),
                    }));

        handle.createUpdate(
                "INSERT INTO record (id, point, linestring, polygon) VALUES (:id, :point, :lineString, :polygon)")
            .bindBean(record)
            .execute();

        PostgisRecord result = handle
            .createQuery("SELECT * FROM record ORDER BY id")
            .mapToBean(PostgisRecord.class)
            .first();

        assertThat(record.getPoint()).isEqualTo(result.getPoint());
        assertThat(record.getLineString()).isEqualTo(result.getLineString());
        assertThat(record.getPolygon()).isEqualTo(result.getPolygon());
    }

    @Test
    void postgisNullSafetyTest() {
        PostgisRecord record = new PostgisRecord();
        record.setId(2);

        handle.createUpdate(
                "INSERT INTO record (id, point, linestring, polygon) VALUES (:id, :point, :lineString, :polygon)")
            .bindBean(record)
            .execute();

        PostgisRecord result = handle
            .createQuery("SELECT * FROM record ORDER BY id")
            .mapToBean(PostgisRecord.class)
            .first();

        assertThat(result.getPoint()).isNull();
        assertThat(result.getLineString()).isNull();
        assertThat(result.getPolygon()).isNull();
    }

    public static final class PostgisRecord {

        private Integer id;
        private Point point;
        private LineString lineString;
        private Polygon polygon;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public Point getPoint() {
            return point;
        }

        public void setPoint(Point point) {
            this.point = point;
        }

        public LineString getLineString() {
            return lineString;
        }

        public void setLineString(LineString lineString) {
            this.lineString = lineString;
        }

        public Polygon getPolygon() {
            return polygon;
        }

        public void setPolygon(Polygon polygon) {
            this.polygon = polygon;
        }
    }
}
