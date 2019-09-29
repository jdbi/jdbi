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
package org.jdbi.v3.benchmark;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jdbi.v3.core.internal.exceptions.Unchecked;
import org.jdbi.v3.core.statement.PreparedBatch;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.BindMap;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlCall;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.testing.JdbiRule;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@Measurement(time = 5)
@Warmup(time = 2)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(1)
public class BeanBindingBenchmark {
    private static final int INNER_LOOPS = 10_000;
    private final Random random = new Random();
    private JdbiRule db;
    private Dao dao;
    private List<SampleBean> sampleList;

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
            .include(BeanBindingBenchmark.class.getSimpleName())
            .forks(0)
            .build();
        new Runner(options).run();
    }

    @Setup
    public void setup() throws Throwable {
        db = JdbiRule.h2()
                .withPlugin(new SqlObjectPlugin());
        db.before();
        dao = db.getHandle()
                .attach(Dao.class);
        dao.create();
        sampleList = Stream.generate(SampleBean::new)
                .limit(50000)
                .collect(Collectors.toList());
    }

    @TearDown(Level.Iteration)
    public void truncateTable() {
        dao.truncateTable();
    }

    @TearDown
    public void close() {
        db.after();
    }

    @Benchmark
    public void batchJdbc() throws SQLException {
        Connection c = db.getHandle().getConnection();
        PreparedStatement ps = c.prepareStatement("insert into sample_table values (?, ?, ?, ?, ?, ?, ?)");
        sampleList.forEach(Unchecked.consumer(sampleBean -> {
            ps.setString(1, sampleBean.getField1());
            ps.setString(2, sampleBean.getField2());
            ps.setInt(3, sampleBean.getField3());
            ps.setInt(4, sampleBean.getField4());
            ps.setInt(5, sampleBean.getField5());
            ps.setInt(6, sampleBean.getField6());
            ps.setInt(7, sampleBean.getField7());
            ps.addBatch();
        }));
        ps.executeBatch();
        c.commit();
    }

    @Benchmark
    public void oneJdbc() throws SQLException {
        Connection c = db.getHandle().getConnection();
        for (int i = 0; i < INNER_LOOPS; i++) {
            PreparedStatement ps = c.prepareStatement("insert into sample_table values (?, ?, ?, ?, ?, ?, ?)");
            SampleBean sampleBean = sampleList.get(0);
            ps.setString(1, sampleBean.getField1());
            ps.setString(2, sampleBean.getField2());
            ps.setInt(3, sampleBean.getField3());
            ps.setInt(4, sampleBean.getField4());
            ps.setInt(5, sampleBean.getField5());
            ps.setInt(6, sampleBean.getField6());
            ps.setInt(7, sampleBean.getField7());
            ps.execute();
            c.commit();
        }
    }

    @Benchmark
    public void batchJdbiBean() {
        dao.batchBindBean(sampleList);
    }

    @Benchmark
    public void oneJdbi() {
        for (int i = 0; i < INNER_LOOPS; i++) {
            dao.bindBean(sampleList.get(0));
        }
    }

    @Benchmark
    public void batchJdbiNamed() {
        dao.useHandle(h -> {
            PreparedBatch b = h.prepareBatch(Dao.INSERT_NAMED);
            sampleList.forEach(sampleBean -> {
                b.bind("field1", sampleBean.getField1());
                b.bind("field2", sampleBean.getField2());
                b.bind("field3", sampleBean.getField3());
                b.bind("field4", sampleBean.getField4());
                b.bind("field5", sampleBean.getField5());
                b.bind("field6", sampleBean.getField6());
                b.bind("field7", sampleBean.getField7());
                b.add();
            });
            b.execute();
        });
    }

    @Benchmark
    public void batchJdbiPositional() {
        dao.useHandle(h -> {
            PreparedBatch b = h.prepareBatch(Dao.INSERT_POS);
            sampleList.forEach(sampleBean -> {
                b.bind(0, sampleBean.getField1());
                b.bind(1, sampleBean.getField2());
                b.bind(2, sampleBean.getField3());
                b.bind(3, sampleBean.getField4());
                b.bind(4, sampleBean.getField5());
                b.bind(5, sampleBean.getField6());
                b.bind(6, sampleBean.getField7());
                b.add();
            });
            b.execute();
        });
    }

    @Benchmark
    public void batchJdbiMap() {
        dao.batchBindMap(sampleList.stream()
                .map(SampleBean::toMap)
                .iterator());
    }

    public String randomString(int targetStringLength) {
        return random.ints('a', 'z')
            .limit(targetStringLength)
            .mapToObj(c -> Character.valueOf((char) c))
            .reduce("", (buf, chr) -> buf + chr, (a, b) -> a + b);
    }

    public int randomInt(int min, int max) {
        return random.nextInt((max - min) + 1) + min;
    }

    public class SampleBean {
        String field1 = randomString(randomInt(2, 30));
        String field2 = randomString(randomInt(2, 30));
        Integer field3 = randomInt(0, 9000000);
        Integer field4 = randomInt(0, 9000000);
        Integer field5 = randomInt(0, 9000000);
        Integer field6 = randomInt(0, 9000000);
        Integer field7 = randomInt(0, 9000000);

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("field1", field1);
            map.put("field2", field2);
            map.put("field3", field3);
            map.put("field4", field4);
            map.put("field5", field5);
            map.put("field6", field6);
            map.put("field7", field7);
            return map;
        }

        public String getField1() {
            return field1;
        }

        public String getField2() {
            return field2;
        }

        public Integer getField3() {
            return field3;
        }

        public Integer getField4() {
            return field4;
        }

        public Integer getField5() {
            return field5;
        }

        public Integer getField6() {
            return field6;
        }

        public Integer getField7() {
            return field7;
        }
    }

    public interface Dao extends SqlObject {
        String INSERT_NAMED = "insert into sample_table values (:field1, :field2, :field3, :field4, :field5, :field6, :field7)";
        String INSERT_POS = "insert into sample_table values (?, ?, ?, ?, ?, ?, ?)";

        @SqlUpdate("create table sample_table(field1 varchar, field2 varchar, field3 int, field4 int, field5 int, field6 int, field7 int)")
        void create();

        @SqlUpdate(INSERT_NAMED)
        void bindBean(@BindBean SampleBean sampleBean);

        @SqlBatch(INSERT_NAMED)
        void batchBindBean(@BindBean List<SampleBean> sampleBeanList);

        @SqlBatch(INSERT_NAMED)
        void batchBindMap(@BindMap Iterator<Map<String, Object>> sampleBeanMapList);

        @SqlCall("truncate table sample_table")
        void truncateTable();
    }
}
