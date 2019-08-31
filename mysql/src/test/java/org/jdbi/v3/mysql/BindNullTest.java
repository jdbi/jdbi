package org.jdbi.v3.mysql;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.assertj.core.api.Assertions;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.stringtemplate4.StringTemplateEngine;
import org.jdbi.v3.testing.JdbiRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BindNullTest {
    @Rule
    public JdbiRule rule = new MariaDBDatabaseRule();

    Jdbi db;
    Handle h;

    @Before
    public void setup() {
        db = rule.getJdbi();
        db = Jdbi.create("jdbc:mysql://root@localhost:3306/test");
        h = db.open();
    }

    @After
    public void close() {
        if (h != null) {
            h.close();
        }
    }

    @Test
    public void bindNull() {
        Beany expected = new Beany();
        expected.setId(1);
        h.setTemplateEngine(new StringTemplateEngine());
        h.execute("create table if not exists beany (id int, at timestamp not null default current_timestamp)");
        h.execute("delete from beany");
        h.createUpdate("insert into beany ("
                    + "id"
                    + "<if(at)> , at <endif>"
                + ") values("
                    + ":id"
                    + "<if(at)> , :at <endif> )")
            .bindBean(expected)
            .defineNamedBindings()
            .execute();
        assertThat(h.createQuery("select * from beany")
                .mapToBean(Beany.class)
                .one())
            .satisfies(b -> {
                assertThat(b.getId()).isEqualTo(1);
                assertThat(b.getAt()).isCloseTo(Instant.now(), Assertions.within(24, ChronoUnit.HOURS));
            });
    }

    public static class Beany {
        private int id;
        private Instant at;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public Instant getAt() {
            return at;
        }

        public void setAt(Instant at) {
            this.at = at;
        }
    }
}