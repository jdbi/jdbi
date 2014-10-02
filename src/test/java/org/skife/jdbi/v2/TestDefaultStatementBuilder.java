package org.skife.jdbi.v2;

import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;

public class TestDefaultStatementBuilder {
    private DefaultStatementBuilder instance;

    private static final String TEST_SQL = "SELECT ... (ignored)";

    private Connection mockConn;
    private StatementContext mockContext;
    private PreparedStatement mockPreparedStatement;

    @Before
    public void setUp() throws Exception {
        instance = new DefaultStatementBuilder();

        mockConn = EasyMock.createMock(Connection.class);
        mockContext = EasyMock.createMock(StatementContext.class);
        mockPreparedStatement = EasyMock.createMock(PreparedStatement.class);
    }

    public static interface DAO
    {
        @GetGeneratedKeys
        public int insertReturningGeneratedKeysWithDefaultColumns();

        @GetGeneratedKeys(columns = "other_id")
        public int insertReturningGeneratedKeysWithColumnsOverride();

    }

    @Test
    public void testCreateNotReturningGeneratedKeys() throws Exception {
        // Set up
        expect(mockContext.isReturningGeneratedKeys()).andReturn(false);
        expect(mockConn.prepareStatement(TEST_SQL)).andReturn(mockPreparedStatement);
        replay(mockContext, mockConn);

        // Test
        final PreparedStatement actualPreparedStatement =
            instance.create(mockConn, TEST_SQL, mockContext);

        // Verify
        assertThat(actualPreparedStatement, sameInstance(mockPreparedStatement));
        verify(mockContext, mockConn);
    }

    @Test
    public void testCreateReturningGeneratedKeysWithDefaultColumns() throws Exception {
        // Set up
        expect(mockContext.isReturningGeneratedKeys()).andReturn(true);

        expect(mockContext.getSqlObjectMethod()).
        andReturn(DAO.class.getMethod("insertReturningGeneratedKeysWithDefaultColumns"));

        expect(mockConn.prepareStatement(TEST_SQL, Statement.RETURN_GENERATED_KEYS)).
        andReturn(mockPreparedStatement);

        replay(mockContext, mockConn);

        // Test
        final PreparedStatement actualPreparedStatement =
            instance.create(mockConn, TEST_SQL, mockContext);

        // Verify
        assertThat(actualPreparedStatement, sameInstance(mockPreparedStatement));
        verify(mockContext, mockConn);
    }

    @Test
    public void testCreateReturningGeneratedKeysWithColumnsOverride() throws Exception {
        // Set up
        expect(mockContext.isReturningGeneratedKeys()).andReturn(true);

        expect(mockContext.getSqlObjectMethod()).
        andReturn(DAO.class.getMethod("insertReturningGeneratedKeysWithColumnsOverride"));

        expect(mockConn.prepareStatement(eq(TEST_SQL), aryEq(new String[] {"other_id"}))).
        andReturn(mockPreparedStatement);

        replay(mockContext, mockConn);

        // Test
        final PreparedStatement actualPreparedStatement =
            instance.create(mockConn, TEST_SQL, mockContext);

        // Verify
        assertThat(actualPreparedStatement, sameInstance(mockPreparedStatement));
        verify(mockContext, mockConn);
    }
}
