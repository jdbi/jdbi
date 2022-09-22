package org.jdbi.v3.core;

import com.google.common.base.Stopwatch;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.SQLException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ArgumentBinderPerf {
    public static void main(String[] args) {
        Stopwatch sw = Stopwatch.createStarted();
        Jdbi.create(new ConnectionFactory() {
            @Override
            public Connection openConnection() throws SQLException {
                Connection conn = mock(Connection.class);
//                when(conn.prepareStatement(anyString()))
                return conn;
            }
        });
    }
}
