package org.jdbi.v3.sqlobject;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.StatementCustomizer;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizer;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizerFactory;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizingAnnotation;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.junit.Rule;
import org.junit.Test;

import static java.util.Arrays.asList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * see https://github.com/jdbi/jdbi/issues/1516
 */
public class TestSqlBatchWithCustomizer {
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    @Test
    public void test() {
        Handle h = db.getSharedHandle();

        h.execute("create table things (thing varchar)");

        h.attach(ThingDAO.class).batchInsertThings(asList("foo", "bar"));

        assertThat(h.createQuery("select thing from things").mapTo(String.class).list())
            .containsExactlyInAnyOrder("foo", "bar");
    }

    public interface ThingDAO {
        @SqlBatch("insert into things (thing) values (:things)")
        @DemandWritable
        void batchInsertThings(@Bind List<String> things);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    @SqlStatementCustomizingAnnotation(DemandWritableCheckFactory.class)
    public @interface DemandWritable {}

    public static class DemandWritableCheckFactory implements SqlStatementCustomizerFactory {
        @Override
        public SqlStatementCustomizer createForMethod(Annotation annotation, Class<?> sqlObjectType, Method method) {
            return stmt -> stmt.addCustomizer(new DemandWritableCheck());
        }
    }

    public static class DemandWritableCheck implements StatementCustomizer {
        @Override
        public void beforeExecution(PreparedStatement stmt, StatementContext ctx) throws SQLException {
            if (stmt.getConnection().isReadOnly()) {
                throw new SQLException("can not perform this operation on a readonly connection");
            }
        }
    }
}
