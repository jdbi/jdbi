package org.skife.jdbi.v2;

import org.skife.jdbi.v2.JdbiCleanables.JdbiCleanable;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.skife.jdbi.v2.tweak.BaseStatementCustomizer;
import org.skife.jdbi.v2.tweak.StatementCustomizer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

abstract class AbstractJdbiStatement
{
    private final List<JdbiCleanable> cleanables = new ArrayList<JdbiCleanable>();
    private final Collection<StatementCustomizer> customizers = new ArrayList<StatementCustomizer>();
    private final ConcreteStatementContext context;

    protected AbstractJdbiStatement(final ConcreteStatementContext context)
    {
        this.context = context;
        addCustomizer(new StatementCleaningCustomizer());
    }

    protected ConcreteStatementContext getConcreteContext()
    {
        return this.context;
    }

    /**
     * Obtain the statement context associated with this statement
     */
    public final StatementContext getContext()
    {
        return context;
    }

    protected void addCustomizers(final Collection<StatementCustomizer> customizers)
    {
        this.customizers.addAll(customizers);
    }

    protected void addCustomizer(final StatementCustomizer customizer)
    {
        this.customizers.add(customizer);
    }

    protected Collection<StatementCustomizer> getStatementCustomizers()
    {
        return this.customizers;
    }

    protected final void beforeExecution(final PreparedStatement stmt)
    {
        for (StatementCustomizer customizer : customizers) {
            try {
                customizer.beforeExecution(stmt, context);
            }
            catch (SQLException e) {
                throw new UnableToExecuteStatementException("Exception thrown in statement customization", e, context);
            }
        }
    }

    protected final void afterExecution(final PreparedStatement stmt)
    {
        for (StatementCustomizer customizer : customizers) {
            try {
                customizer.afterExecution(stmt, context);
            }
            catch (SQLException e) {
                throw new UnableToExecuteStatementException("Exception thrown in statement customization", e, context);
            }
        }
    }

    protected final void cleanup()
    {
        for (StatementCustomizer customizer : customizers) {
            try {
                customizer.cleanup(context);
            }
            catch (SQLException e) {
                throw new UnableToExecuteStatementException("Could not clean up", e, context);
            }
        }
    }

    protected void addCleanable(final JdbiCleanable cleanable)
    {
        cleanables.add(cleanable);
    }

    class StatementCleaningCustomizer extends BaseStatementCustomizer
    {
        @Override
        public final void cleanup(final StatementContext ctx)
            throws SQLException
        {
            SQLException lastException = null;
            try {
                Collections.reverse(cleanables);
                for (JdbiCleanable cleanable : cleanables) {
                    try {
                        cleanable.cleanup();
                    }
                    catch (SQLException sqlException) {
                        lastException = sqlException;
                    }
                }
                cleanables.clear();
            }
            finally {
                if (lastException != null)  {
                    throw lastException;
                }
            }
        }
    }
}

