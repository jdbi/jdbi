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
package org.jdbi.nativedemo;

import java.security.SecureRandom;
import java.util.UUID;

import org.jdbi.core.Jdbi;
import org.jdbi.core.mapper.reflect.ConstructorMapper;
import org.jdbi.sqlobject.SqlObjectPlugin;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.sqlobject.statement.SqlUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

public final class NativeMain {

    private static final Logger LOG = LoggerFactory.getLogger(NativeMain.class);

    public static void main(String[] args) throws Exception {
        NativeMain nativeMain = new NativeMain();
        var jdbi = nativeMain.init();
        nativeMain.execute(jdbi);
    }

    private NativeMain() {
    }

    Jdbi init() throws Exception {
        var url = "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        var jdbi = Jdbi.create(url);
        jdbi.installPlugin(new SqlObjectPlugin());

        jdbi.useExtension(CreateDao.class, CreateDao::createTable);
        jdbi.registerRowMapper(Demo.class, ConstructorMapper.of(Demo.class));

        var random = SecureRandom.getInstanceStrong();

        jdbi.useExtension(CreateDao.class, dao -> {
            for (int i = 0; i < 1000; i++) {
                dao.insert(i, "User Name" + UUID.randomUUID(), random.nextInt(10, 100));
            }
        });

        return jdbi;
    }

    void execute(Jdbi jdbi) throws Exception {
        var random = SecureRandom.getInstanceStrong();
        jdbi.useExtension(QueryDao.class, dao -> {
            for (int i = 0; i < 1000; i++) {
                var id = random.nextInt(1000);
                var demo = dao.getObject(id);
                LOG.info(format("Demo %d: %s", id, demo));
                Thread.sleep(200L);

                var start = random.nextInt(1000);
                var end = random.nextInt(1000, 2000);
                var sum = dao.getRange(start, end);
                LOG.info(format("Sum of %d to %d: %d", start, end, sum));
                Thread.sleep(200L);
            }
        });
    }

    public record Demo(int id, String name, int count) {}

    public interface CreateDao {

        @SqlUpdate("CREATE TABLE demo (id INTEGER PRIMARY KEY, name VARCHAR, count INTEGER)")
        void createTable();

        @SqlUpdate("INSERT INTO demo (id, name, count) VALUES (:id, :name, :count)")
        void insert(int id, String name, int count);
    }

    public interface QueryDao {

        @SqlQuery("SELECT * FROM demo WHERE id = :id")
        Demo getObject(int id);

        @SqlQuery("SELECT SUM(count) FROM demo WHERE id > :start AND id < :end")
        int getRange(int start, int end);
    }
}
