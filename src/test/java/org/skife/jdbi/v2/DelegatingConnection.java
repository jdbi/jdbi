/*
 * Copyright 2004-2007 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.skife.jdbi.v2;

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

public class DelegatingConnection implements Connection
{
    private Connection connection;

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

    public void setAutoCommit(boolean b) throws SQLException
    {
        connection.setAutoCommit(b);
    }

    public boolean getAutoCommit() throws SQLException
    {
        return connection.getAutoCommit();
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

    public void setReadOnly(boolean b) throws SQLException
    {
        connection.setReadOnly(b);
    }

    public boolean isReadOnly() throws SQLException
    {
        return connection.isReadOnly();
    }

    public void setCatalog(String s) throws SQLException
    {
        connection.setCatalog(s);
    }

    public String getCatalog() throws SQLException
    {
        return connection.getCatalog();
    }

    public void setTransactionIsolation(int i) throws SQLException
    {
        connection.setTransactionIsolation(i);
    }

    public int getTransactionIsolation() throws SQLException
    {
        return connection.getTransactionIsolation();
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

    public void setHoldability(int i) throws SQLException
    {
        connection.setHoldability(i);
    }

    public int getHoldability() throws SQLException
    {
        return connection.getHoldability();
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
		return null;
	}

	public Blob createBlob() throws SQLException
	{
		return null;
	}

	public NClob createNClob() throws SQLException
	{
		return null;
	}

	public SQLXML createSQLXML() throws SQLException
	{
		return null;
	}

	public boolean isValid(int timeout) throws SQLException
	{
		return false;
	}

	public void setClientInfo(String name, String value) throws SQLClientInfoException
	{

	}

	public void setClientInfo(Properties properties) throws SQLClientInfoException
	{

	}

	public String getClientInfo(String name) throws SQLException
	{
		return null;
	}

	public Properties getClientInfo() throws SQLException
	{
		return null;
	}

	public Array createArrayOf(String typeName, Object[] elements) throws SQLException
	{
		return null;
	}

	public Struct createStruct(String typeName, Object[] attributes) throws SQLException
	{
		return null;
	}

	public <T> T unwrap(Class<T> iface) throws SQLException
	{
		return null;
	}

	public boolean isWrapperFor(Class<?> iface) throws SQLException
	{
		return false;
	}
}
