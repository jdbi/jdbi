package org.skife.jdbi.v2;

import java.sql.PreparedStatement;

import org.easymock.classextension.EasyMock;
import org.skife.jdbi.v2.tweak.Argument;

public class TestBooleanIntegerArgument {

    public void testTrue() throws Exception {

        PreparedStatement mockStmt = EasyMock.createMock(PreparedStatement.class);
        mockStmt.setInt(5, 1);

        EasyMock.replay(mockStmt);

        Argument arrrgh = new BooleanIntegerArgument(true);

        arrrgh.apply(5, mockStmt, null);

        EasyMock.verify(mockStmt);
    }

    public void testFalse() throws Exception {

        PreparedStatement mockStmt = EasyMock.createMock(PreparedStatement.class);
        mockStmt.setInt(5, 0);

        EasyMock.replay(mockStmt);

        Argument arrrgh = new BooleanIntegerArgument(false);

        arrrgh.apply(5, mockStmt, null);

        EasyMock.verify(mockStmt);
    }
}
