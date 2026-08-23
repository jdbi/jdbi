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

    private static final int ROW_COUNT = 1000;
    private static final int DEFAULT_QUERY_COUNT = 1000;

    public static void main(String[] args) throws Exception {
        // every query pauses, so a default run takes minutes; pass a count for a short one
        var queryCount = args.length > 0 ? parseQueryCount(args[0]) : DEFAULT_QUERY_COUNT;

        NativeMain nativeMain = new NativeMain();
        var jdbi = nativeMain.init();
        nativeMain.execute(jdbi, queryCount);
    }

    private NativeMain() {
    }

    // A count of zero would let the CI check pass while proving nothing ran, so reject it
    // here rather than let the demo exit successfully having executed no query.
    private static int parseQueryCount(String arg) {
        int count;
        try {
            count = Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(format("query count '%s' is not a number", arg), e);
        }
        if (count < 1) {
            throw new IllegalArgumentException(format("query count must be at least 1, got %d", count));
        }
        return count;
    }

    Jdbi init() throws Exception {
        var url = "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        var jdbi = Jdbi.create(url);
        jdbi.installPlugin(new SqlObjectPlugin());

        jdbi.useExtension(CreateDao.class, CreateDao::createTable);
        jdbi.registerRowMapper(Demo.class, ConstructorMapper.of(Demo.class));

        var random = SecureRandom.getInstanceStrong();

        jdbi.useExtension(CreateDao.class, dao -> {
            for (int i = 0; i < ROW_COUNT; i++) {
                dao.insert(i, "User Name" + UUID.randomUUID(), random.nextInt(10, 100));
            }
        });

        return jdbi;
    }

    void execute(Jdbi jdbi, int queryCount) throws Exception {
        var random = SecureRandom.getInstanceStrong();
        jdbi.useExtension(QueryDao.class, dao -> {
            for (int i = 0; i < queryCount; i++) {
                var id = random.nextInt(ROW_COUNT);
                var demo = dao.getObject(id);
                LOG.info(format("Demo %d: %s", id, demo));
                Thread.sleep(200L);

                var start = random.nextInt(ROW_COUNT);
                var end = random.nextInt(ROW_COUNT, 2 * ROW_COUNT);
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
