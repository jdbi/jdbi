/*
 * Copyright (C) 2004 - 2013 Brian McCallister
 *
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
package org.skife.jdbi.v2;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * Convenience class for intercepting Connection behavior.
 */
public class DelegatingConnection implements Connection
{
    private final Connection connection;

    public DelegatingConnection(Connection delegate)
    {
        this.connection = delegate;
    }

    public Statement createStatement() throws SQLException
    {
        return connection.createStatement();
    }

    public PreparedStatement prepareStatement(String s) throws SQLException
    {
        return connection.prepareStatement(s);
    }

    public CallableStatement prepareCall(String s) throws SQLException
    {
        return connection.prepareCall(s);
    }

    public String nativeSQL(String s) throws SQLException
    {
        return connection.nativeSQL(s);
    }

    public boolean getAutoCommit() throws SQLException
    {
        return connection.getAutoCommit();
    }

    public void setAutoCommit(boolean b) throws SQLException
    {
        connection.setAutoCommit(b);
    }

    public void commit() throws SQLException
    {
        connection.commit();
    }

    public void rollback() throws SQLException
    {
        connection.rollback();
    }

    public void close() throws SQLException
    {
        connection.close();
    }

    public boolean isClosed() throws SQLException
    {
        return connection.isClosed();
    }

    public DatabaseMetaData getMetaData() throws SQLException
    {
        return connection.getMetaData();
    }

    public boolean isReadOnly() throws SQLException
    {
        return connection.isReadOnly();
    }

    public void setReadOnly(boolean b) throws SQLException
    {
        connection.setReadOnly(b);
    }

    public String getCatalog() throws SQLException
    {
        return connection.getCatalog();
    }

    public void setCatalog(String s) throws SQLException
    {
        connection.setCatalog(s);
    }

    public int getTransactionIsolation() throws SQLException
    {
        return connection.getTransactionIsolation();
    }

    public void setTransactionIsolation(int i) throws SQLException
    {
        connection.setTransactionIsolation(i);
    }

    public SQLWarning getWarnings() throws SQLException
    {
        return connection.getWarnings();
    }

    public void clearWarnings() throws SQLException
    {
        connection.clearWarnings();
    }

    public Statement createStatement(int i, int i1) throws SQLException
    {
        return connection.createStatement(i, i1);
    }

    public PreparedStatement prepareStatement(String s, int i, int i1) throws SQLException
    {
        return connection.prepareStatement(s, i, i1);
    }

    public CallableStatement prepareCall(String s, int i, int i1) throws SQLException
    {
        return connection.prepareCall(s, i, i1);
    }

    public Map<String, Class<?>> getTypeMap() throws SQLException
    {
        return connection.getTypeMap();
    }

    public void setTypeMap(Map<String, Class<?>> map) throws SQLException
    {
        connection.setTypeMap(map);
    }

    public int getHoldability() throws SQLException
    {
        return connection.getHoldability();
    }

    public void setHoldability(int i) throws SQLException
    {
        connection.setHoldability(i);
    }

    public Savepoint setSavepoint() throws SQLException
    {
        return connection.setSavepoint();
    }

    public Savepoint setSavepoint(String s) throws SQLException
    {
        return connection.setSavepoint(s);
    }

    public void rollback(Savepoint savepoint) throws SQLException
    {
        connection.rollback(savepoint);
    }

    public void releaseSavepoint(Savepoint savepoint) throws SQLException
    {
        connection.releaseSavepoint(savepoint);
    }

    public Statement createStatement(int i, int i1, int i2) throws SQLException
    {
        return connection.createStatement(i, i1, i2);
    }

    public PreparedStatement prepareStatement(String s, int i, int i1, int i2) throws SQLException
    {
        return connection.prepareStatement(s, i, i1, i2);
    }

    public CallableStatement prepareCall(String s, int i, int i1, int i2) throws SQLException
    {
        return connection.prepareCall(s, i, i1, i2);
    }

    public PreparedStatement prepareStatement(String s, int i) throws SQLException
    {
        return connection.prepareStatement(s, i);
    }

    public PreparedStatement prepareStatement(String s, int[] ints) throws SQLException
    {
        return connection.prepareStatement(s, ints);
    }

    public PreparedStatement prepareStatement(String s, String[] strings) throws SQLException
    {
        return connection.prepareStatement(s, strings);
    }

    public Clob createClob() throws SQLException
    {
        return connection.createClob();
    }

    public Blob createBlob() throws SQLException
    {
        return connection.createBlob();
    }

    public NClob createNClob() throws SQLException
    {
        return connection.createNClob();
    }

    public SQLXML createSQLXML() throws SQLException
    {
        return connection.createSQLXML();
    }

    public boolean isValid(int timeout) throws SQLException
    {
        return connection.isValid(timeout);
    }

    public void setClientInfo(String name, String value) throws SQLClientInfoException
    {
        connection.setClientInfo(name, value);
    }

    public String getClientInfo(String name) throws SQLException
    {
        return connection.getClientInfo(name);
    }

    public Properties getClientInfo() throws SQLException
    {
        return connection.getClientInfo();
    }

    public void setClientInfo(Properties properties) throws SQLClientInfoException
    {
        connection.setClientInfo(properties);
    }

    public Array createArrayOf(String typeName, Object[] elements) throws SQLException
    {
        return connection.createArrayOf(typeName, elements);
    }

    public Struct createStruct(String typeName, Object[] attributes) throws SQLException
    {
        return connection.createStruct(typeName, attributes);
    }

    public <T> T unwrap(Class<T> iface) throws SQLException
    {
        return connection.unwrap(iface);
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException
    {
        return connection.isWrapperFor(iface);
    }

    public String getSchema() throws SQLException
    {
        try {
            Method m = connection.getClass().getDeclaredMethod("getSchema");
            return (String) m.invoke(connection);
        }
        catch (NoSuchMethodException e) {
            throw new IllegalStateException("getSchema does not exist in this Java version");
        }
        catch (InvocationTargetException e) {
            if (e.getCause() instanceof SQLException) {
                throw (SQLException) e.getCause();
            }
            else {
                throw new IllegalStateException(e);
            }
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void setSchema(String schema) throws SQLException
    {
        try {
            Method m = connection.getClass().getDeclaredMethod("setSchema", String.class);
            m.invoke(connection, schema);
        }
        catch (NoSuchMethodException e) {
            throw new IllegalStateException("setSchema does not exist in this Java version");
        }
        catch (InvocationTargetException e) {
            if (e.getCause() instanceof SQLException) {
                throw (SQLException) e.getCause();
            }
            else {
                throw new IllegalStateException(e);
            }
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void abort(Executor executor) throws SQLException
    {
        try {
            Method m = connection.getClass().getDeclaredMethod("abort");
            m.invoke(connection, executor);
        }
        catch (NoSuchMethodException e) {
            throw new IllegalStateException("abort does not exist in this Java version");
        }
        catch (InvocationTargetException e) {
            if (e.getCause() instanceof SQLException) {
                throw (SQLException) e.getCause();
            }
            else {
                throw new IllegalStateException(e);
            }
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException
    {
        try {
            Method m = connection.getClass().getDeclaredMethod("setNetworkTimeout");
            m.invoke(connection, executor, milliseconds);
        }
        catch (NoSuchMethodException e) {
            throw new IllegalStateException("setNetworkTimeout does not exist in this Java version");
        }
        catch (InvocationTargetException e) {
            if (e.getCause() instanceof SQLException) {
                throw (SQLException) e.getCause();
            }
            else {
                throw new IllegalStateException(e);
            }
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public int getNetworkTimeout() throws SQLException
    {
        try {
            Method m = connection.getClass().getDeclaredMethod("getNetworkTimeout");
            return (Integer) m.invoke(connection);
        }
        catch (NoSuchMethodException e) {
            throw new IllegalStateException("getNetworkTimeout does not exist in this Java version");
        }
        catch (InvocationTargetException e) {
            if (e.getCause() instanceof SQLException) {
                throw (SQLException) e.getCause();
            }
            else {
                throw new IllegalStateException(e);
            }
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
