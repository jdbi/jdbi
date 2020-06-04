package org.jdbi.v3.postgres.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.StatementCustomizer;
import org.jdbi.v3.postgres.DeleteLob;
import org.jdbi.v3.postgres.PgLobApi;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizer;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizerFactory;

public class DeleteLobCustomizerFactory implements SqlStatementCustomizerFactory {
    @Override
    public SqlStatementCustomizer createForMethod(Annotation annotation, Class<?> sqlObjectType, Method method) {
        return stmt -> new Customizer((DeleteLob) annotation, stmt.getHandle());
    }

    class Customizer implements StatementCustomizer {
        private final DeleteLob annotation;
        private final Handle handle;

        Customizer(DeleteLob annotation, Handle handle) {
            this.annotation = annotation;
            this.handle = handle;
        }

        @Override
        public void afterExecution(PreparedStatement stmt, StatementContext ctx) throws SQLException {
            final long oid;
            String lobOidKey = annotation.lobOidKey();
            if (lobOidKey.isEmpty()) {
                oid = stmt.getGeneratedKeys().getLong(1);
            } else {
                oid = stmt.getGeneratedKeys().getLong(lobOidKey);
            }
            handle.attach(PgLobApi.class).deleteLob(oid);
        }
    }
}
