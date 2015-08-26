/*
 * Copyright (C) 2015 Zane Benefits
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

import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

/**
 * A ResultSet wrapper which exposes a single column to the caller.
 */
// TODO should mutating methods throw an exception?
class SingleColumnResultSetView implements ResultSet {
    private final ResultSet delegate;
    private final int targetIndex;
    private final String targetLabel;
    private final SingleColumnMetadata metadata;

    SingleColumnResultSetView(ResultSet delegate, int targetIndex) throws SQLException {
        this.delegate = delegate;
        this.targetIndex = targetIndex;
        this.targetLabel = delegate.getMetaData().getColumnLabel(targetIndex);
        this.metadata = new SingleColumnMetadata(delegate.getMetaData());
    }

    void checkIndex(int columnIndex) throws SQLException {
        if (columnIndex != 1) {
            throw new SQLException("Column index " + columnIndex + " out of range.");
        }
    }

    void checkLabel(String columnLabel) throws SQLException {
        if (!targetLabel.equals(columnLabel)) {
            throw new SQLException("Column label " + columnLabel + " not found.");
        }
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return metadata;
    }

    @Override
    public int findColumn(String columnLabel) throws SQLException {
        checkLabel(columnLabel);
        return 1;
    }

    @Override
    public boolean wasNull() throws SQLException {
        return delegate.wasNull();
    }

    @Override
    public boolean getBoolean(int columnIndex) throws SQLException {
        checkIndex(columnIndex);
        return delegate.getBoolean(targetIndex);
    }

    @Override
    public boolean getBoolean(String columnLabel) throws SQLException {
        checkLabel(columnLabel);
        return delegate.getBoolean(targetIndex);
    }

    @Override
    public byte getByte(int columnIndex) throws SQLException {
        checkIndex(columnIndex);
        return delegate.getByte(targetIndex);
    }

    @Override
    public byte getByte(String columnLabel) throws SQLException {
        checkLabel(columnLabel);
        return delegate.getByte(targetIndex);
    }

    @Override
    public short getShort(int columnIndex) throws SQLException {
        checkIndex(columnIndex);
        return delegate.getShort(targetIndex);
    }

    @Override
    public short getShort(String columnLabel) throws SQLException {
        checkLabel(columnLabel);
        return delegate.getShort(targetIndex);
    }

    @Override
    public int getInt(int columnIndex) throws SQLException {
        checkIndex(columnIndex);
        return delegate.getInt(targetIndex);
    }

    @Override
    public int getInt(String columnLabel) throws SQLException {
        checkLabel(columnLabel);
        return delegate.getInt(targetIndex);
    }

    @Override
    public long getLong(int columnIndex) throws SQLException {
        checkIndex(columnIndex);
        return delegate.getLong(targetIndex);
    }

    @Override
    public long getLong(String columnLabel) throws SQLException {
        checkLabel(columnLabel);
        return delegate.getLong(targetIndex);
    }

    @Override
    public float getFloat(int columnIndex) throws SQLException {
        checkIndex(columnIndex);
        return delegate.getFloat(targetIndex);
    }

    @Override
    public float getFloat(String columnLabel) throws SQLException {
        checkLabel(columnLabel);
        return delegate.getFloat(targetIndex);
    }

    @Override
    public double getDouble(int columnIndex) throws SQLException {
        checkIndex(columnIndex);
        return delegate.getDouble(targetIndex);
    }

    @Override
    public double getDouble(String columnLabel) throws SQLException {
        checkLabel(columnLabel);
        return delegate.getDouble(targetIndex);
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
        checkIndex(columnIndex);
        return delegate.getBigDecimal(targetIndex);
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
        checkIndex(columnIndex);
        return delegate.getBigDecimal(targetIndex, scale);
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
        checkLabel(columnLabel);
        return delegate.getBigDecimal(targetIndex);
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
        checkLabel(columnLabel);
        return delegate.getBigDecimal(targetIndex, scale);
    }

    @Override
    public String getString(int columnIndex) throws SQLException {
        checkIndex(columnIndex);
        return delegate.getString(targetIndex);
    }

    @Override
    public String getString(String columnLabel) throws SQLException {
        checkLabel(columnLabel);
        return delegate.getString(targetIndex);
    }

    @Override
    public String getNString(int columnIndex) throws SQLException {
        checkIndex(columnIndex);
        return delegate.getNString(targetIndex);
    }

    @Override
    public String getNString(String columnLabel) throws SQLException {
        checkLabel(columnLabel);
        return delegate.getNString(targetIndex);
    }

    @Override
    public Clob getClob(int columnIndex) throws SQLException {
        checkIndex(columnIndex);
        return delegate.getClob(targetIndex);
    }

    @Override
    public Clob getClob(String columnLabel) throws SQLException {
        checkLabel(columnLabel);
        return delegate.getClob(targetIndex);
    }

    @Override
    public NClob getNClob(int columnIndex) throws SQLException {
        checkIndex(columnIndex);
        return delegate.getNClob(targetIndex);
    }

    @Override
    public NClob getNClob(String columnLabel) throws SQLException {
        checkLabel(columnLabel);
        return delegate.getNClob(targetIndex);
    }

    @Override
    public Date getDate(int columnIndex) throws SQLException {
        checkIndex(columnIndex);
        return delegate.getDate(targetIndex);
    }

    @Override
    public Date getDate(int columnIndex, Calendar cal) throws SQLException {
        checkIndex(columnIndex);
        return delegate.getDate(targetIndex, cal);
    }

    @Override
    public Date getDate(String columnLabel) throws SQLException {
        checkLabel(columnLabel);
        return delegate.getDate(targetIndex);
    }

    @Override
    public Date getDate(String columnLabel, Calendar cal) throws SQLException {
        checkLabel(columnLabel);
        return delegate.getDate(targetIndex, cal);
    }

    @Override
    public Time getTime(int columnIndex) throws SQLException {
        checkIndex(columnIndex);
        return delegate.getTime(targetIndex);
    }

    @Override
    public Time getTime(int columnIndex, Calendar cal) throws SQLException {
        checkIndex(columnIndex);
        return delegate.getTime(targetIndex, cal);
    }

    @Override
    public Time getTime(String columnLabel) throws SQLException {
        checkLabel(columnLabel);
        return delegate.getTime(targetIndex);
    }

    @Override
    public Time getTime(String columnLabel, Calendar cal) throws SQLException {
        checkLabel(columnLabel);
        return delegate.getTime(targetIndex, cal);
    }

    @Override
    public Timestamp getTimestamp(int columnIndex) throws SQLException {
        checkIndex(columnIndex);
        return delegate.getTimestamp(targetIndex);
    }

    @Override
    public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
        checkIndex(columnIndex);
        return delegate.getTimestamp(targetIndex, cal);
    }

    @Override
    public Timestamp getTimestamp(String columnLabel) throws SQLException {
        checkLabel(columnLabel);
        return delegate.getTimestamp(targetIndex);
    }

    @Override
    public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {
        checkLabel(columnLabel);
        return delegate.getTimestamp(targetIndex, cal);
    }

    @Override
    public Blob getBlob(int columnIndex) throws SQLException {
        checkIndex(columnIndex);
        return delegate.getBlob(targetIndex);
    }

    @Override
    public Blob getBlob(String columnLabel) throws SQLException {
        checkLabel(columnLabel);
        return delegate.getBlob(targetIndex);
    }

    @Override
    public byte[] getBytes(int columnIndex) throws SQLException {
        checkIndex(columnIndex);
        return delegate.getBytes(targetIndex);
    }

    @Override
    public byte[] getBytes(String columnLabel) throws SQLException {
        checkLabel(columnLabel);
        return delegate.getBytes(targetIndex);
    }

    @Override
    public Reader getCharacterStream(int columnIndex) throws SQLException {
        checkIndex(columnIndex);
        return delegate.getCharacterStream(targetIndex);
    }

    @Override
    public Reader getCharacterStream(String columnLabel) throws SQLException {
        checkLabel(columnLabel);
        return delegate.getCharacterStream(targetIndex);
    }

    @Override
    public Reader getNCharacterStream(int columnIndex) throws SQLException {
        checkIndex(columnIndex);
        return delegate.getNCharacterStream(targetIndex);
    }

    @Override
    public Reader getNCharacterStream(String columnLabel) throws SQLException {
        checkLabel(columnLabel);
        return delegate.getNCharacterStream(targetIndex);
    }

    @Override
    public InputStream getUnicodeStream(int columnIndex) throws SQLException {
        checkIndex(columnIndex);
        return delegate.getUnicodeStream(targetIndex);
    }

    @Override
    public InputStream getUnicodeStream(String columnLabel) throws SQLException {
        checkLabel(columnLabel);
        return delegate.getUnicodeStream(targetIndex);
    }

    @Override
    public InputStream getAsciiStream(int columnIndex) throws SQLException {
        checkIndex(columnIndex);
        return delegate.getAsciiStream(targetIndex);
    }

    @Override
    public InputStream getAsciiStream(String columnLabel) throws SQLException {
        checkLabel(columnLabel);
        return delegate.getAsciiStream(targetIndex);
    }

    @Override
    public InputStream getBinaryStream(int columnIndex) throws SQLException {
        checkIndex(columnIndex);
        return delegate.getBinaryStream(targetIndex);
    }

    @Override
    public InputStream getBinaryStream(String columnLabel) throws SQLException {
        checkLabel(columnLabel);
        return delegate.getBinaryStream(targetIndex);
    }

    @Override
    public URL getURL(int columnIndex) throws SQLException {
        checkIndex(columnIndex);
        return delegate.getURL(targetIndex);
    }

    @Override
    public URL getURL(String columnLabel) throws SQLException {
        checkLabel(columnLabel);
        return delegate.getURL(targetIndex);
    }

    @Override
    public Array getArray(int columnIndex) throws SQLException {
        checkIndex(columnIndex);
        return delegate.getArray(targetIndex);
    }

    @Override
    public Array getArray(String columnLabel) throws SQLException {
        checkLabel(columnLabel);
        return delegate.getArray(targetIndex);
    }

    @Override
    public Ref getRef(int columnIndex) throws SQLException {
        checkIndex(columnIndex);
        return delegate.getRef(targetIndex);
    }

    @Override
    public Ref getRef(String columnLabel) throws SQLException {
        checkLabel(columnLabel);
        return delegate.getRef(targetIndex);
    }

    @Override
    public RowId getRowId(int columnIndex) throws SQLException {
        checkIndex(columnIndex);
        return delegate.getRowId(targetIndex);
    }

    @Override
    public RowId getRowId(String columnLabel) throws SQLException {
        checkLabel(columnLabel);
        return delegate.getRowId(targetIndex);
    }

    @Override
    public SQLXML getSQLXML(int columnIndex) throws SQLException {
        checkIndex(columnIndex);
        return delegate.getSQLXML(targetIndex);
    }

    @Override
    public SQLXML getSQLXML(String columnLabel) throws SQLException {
        checkLabel(columnLabel);
        return delegate.getSQLXML(targetIndex);
    }

    @Override
    public Object getObject(int columnIndex) throws SQLException {
        checkIndex(columnIndex);
        return delegate.getObject(targetIndex);
    }

    @Override
    public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
        checkIndex(columnIndex);
        return delegate.getObject(targetIndex, map);
    }

    @Override
    public Object getObject(String columnLabel) throws SQLException {
        checkLabel(columnLabel);
        return delegate.getObject(targetIndex);
    }

    @Override
    public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException {
        checkLabel(columnLabel);
        return delegate.getObject(targetIndex, map);
    }

    // @Override
    /* This method is new in Java 1.7, therefore must omit @Override until we upgrade to 1.7 or later. */
    public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
        checkIndex(columnIndex);
        return invokeGetObject(type);
    }

    // @Override
    /* This method is new in Java 1.7, therefore must omit @Override until we upgrade to 1.7 or later. */
    public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
        checkLabel(columnLabel);
        return invokeGetObject(type);
    }

    @SuppressWarnings("unchecked")
    private <T> T invokeGetObject(Class<T> type) throws SQLException {
        try {
            return (T) ResultSet.class
                    .getMethod("getObject", int.class, Class.class)
                    .invoke(delegate, targetIndex, type);
        } catch (IllegalAccessException e) {
            throw new SQLException("Tried to call JDBC 4.1 method in a pre-4.1 environment");
        } catch (InvocationTargetException e) {
            throw new SQLException("Tried to call JDBC 4.1 method in a pre-4.1 environment");
        } catch (NoSuchMethodException e) {
            throw new SQLException("Tried to call JDBC 4.1 method in a pre-4.1 environment");
        }
    }

    //// data mutators

    @Override
    public void updateNull(int columnIndex) throws SQLException {
        checkIndex(columnIndex);
        delegate.updateNull(targetIndex);
    }

    @Override
    public void updateNull(String columnLabel) throws SQLException {
        checkLabel(columnLabel);
        delegate.updateNull(targetIndex);
    }

    @Override
    public void updateBoolean(int columnIndex, boolean x) throws SQLException {
        checkIndex(columnIndex);
        delegate.updateBoolean(targetIndex, x);
    }

    @Override
    public void updateBoolean(String columnLabel, boolean x) throws SQLException {
        checkLabel(columnLabel);
        delegate.updateBoolean(targetIndex, x);
    }

    @Override
    public void updateByte(int columnIndex, byte x) throws SQLException {
        checkIndex(columnIndex);
        delegate.updateByte(targetIndex, x);
    }

    @Override
    public void updateByte(String columnLabel, byte x) throws SQLException {
        checkLabel(columnLabel);
        delegate.updateByte(targetIndex, x);
    }

    @Override
    public void updateShort(int columnIndex, short x) throws SQLException {
        checkIndex(columnIndex);
        delegate.updateShort(targetIndex, x);
    }

    @Override
    public void updateShort(String columnLabel, short x) throws SQLException {
        checkLabel(columnLabel);
        delegate.updateShort(targetIndex, x);
    }

    @Override
    public void updateInt(int columnIndex, int x) throws SQLException {
        checkIndex(columnIndex);
        delegate.updateInt(targetIndex, x);
    }

    @Override
    public void updateInt(String columnLabel, int x) throws SQLException {
        checkLabel(columnLabel);
        delegate.updateInt(targetIndex, x);
    }

    @Override
    public void updateLong(int columnIndex, long x) throws SQLException {
        checkIndex(columnIndex);
        delegate.updateLong(targetIndex, x);
    }

    @Override
    public void updateLong(String columnLabel, long x) throws SQLException {
        checkLabel(columnLabel);
        delegate.updateLong(targetIndex, x);
    }

    @Override
    public void updateFloat(int columnIndex, float x) throws SQLException {
        checkIndex(columnIndex);
        delegate.updateFloat(targetIndex, x);
    }

    @Override
    public void updateFloat(String columnLabel, float x) throws SQLException {
        checkLabel(columnLabel);
        delegate.updateFloat(targetIndex, x);
    }

    @Override
    public void updateDouble(int columnIndex, double x) throws SQLException {
        checkIndex(columnIndex);
        delegate.updateDouble(targetIndex, x);
    }

    @Override
    public void updateDouble(String columnLabel, double x) throws SQLException {
        checkLabel(columnLabel);
        delegate.updateDouble(targetIndex, x);
    }

    @Override
    public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
        checkIndex(columnIndex);
        delegate.updateBigDecimal(targetIndex, x);
    }

    @Override
    public void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException {
        checkLabel(columnLabel);
        delegate.updateBigDecimal(targetIndex, x);
    }

    @Override
    public void updateString(int columnIndex, String x) throws SQLException {
        checkIndex(columnIndex);
        delegate.updateString(targetIndex, x);
    }

    @Override
    public void updateString(String columnLabel, String x) throws SQLException {
        checkLabel(columnLabel);
        delegate.updateString(targetIndex, x);
    }

    @Override
    public void updateBytes(int columnIndex, byte[] x) throws SQLException {
        checkIndex(columnIndex);
        delegate.updateBytes(targetIndex, x);
    }

    @Override
    public void updateBytes(String columnLabel, byte[] x) throws SQLException {
        checkLabel(columnLabel);
        delegate.updateBytes(targetIndex, x);
    }

    @Override
    public void updateDate(int columnIndex, Date x) throws SQLException {
        checkIndex(columnIndex);
        delegate.updateDate(targetIndex, x);
    }

    @Override
    public void updateDate(String columnLabel, Date x) throws SQLException {
        checkLabel(columnLabel);
        delegate.updateDate(targetIndex, x);
    }

    @Override
    public void updateTime(int columnIndex, Time x) throws SQLException {
        checkIndex(columnIndex);
        delegate.updateTime(targetIndex, x);
    }

    @Override
    public void updateTime(String columnLabel, Time x) throws SQLException {
        checkLabel(columnLabel);
        delegate.updateTime(targetIndex, x);
    }

    @Override
    public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {
        checkIndex(columnIndex);
        delegate.updateTimestamp(targetIndex, x);
    }

    @Override
    public void updateTimestamp(String columnLabel, Timestamp x) throws SQLException {
        checkLabel(columnLabel);
        delegate.updateTimestamp(targetIndex, x);
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {
        checkIndex(columnIndex);
        delegate.updateAsciiStream(targetIndex, x);
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {
        checkIndex(columnIndex);
        delegate.updateAsciiStream(targetIndex, x, length);
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {
        checkIndex(columnIndex);
        delegate.updateAsciiStream(targetIndex, x, length);
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {
        checkLabel(columnLabel);
        delegate.updateAsciiStream(targetIndex, x);
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x, int length) throws SQLException {
        checkLabel(columnLabel);
        delegate.updateAsciiStream(targetIndex, x, length);
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {
        checkLabel(columnLabel);
        delegate.updateAsciiStream(targetIndex, x, length);
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {
        checkIndex(columnIndex);
        delegate.updateBinaryStream(targetIndex, x);
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {
        checkIndex(columnIndex);
        delegate.updateBinaryStream(targetIndex, x, length);
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {
        checkIndex(columnIndex);
        delegate.updateBinaryStream(targetIndex, x, length);
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {
        checkLabel(columnLabel);
        delegate.updateBinaryStream(targetIndex, x);
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x, int length) throws SQLException {
        checkLabel(columnLabel);
        delegate.updateBinaryStream(targetIndex, x, length);
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {
        checkLabel(columnLabel);
        delegate.updateBinaryStream(targetIndex, x, length);
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {
        checkIndex(columnIndex);
        delegate.updateCharacterStream(targetIndex, x, length);
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {
        checkIndex(columnIndex);
        delegate.updateCharacterStream(targetIndex, x);
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
        checkIndex(columnIndex);
        delegate.updateCharacterStream(targetIndex, x, length);
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {
        checkLabel(columnLabel);
        delegate.updateCharacterStream(targetIndex, reader);
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader, int length) throws SQLException {
        checkLabel(columnLabel);
        delegate.updateCharacterStream(targetIndex, reader, length);
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
        checkLabel(columnLabel);
        delegate.updateCharacterStream(targetIndex, reader, length);
    }

    @Override
    public void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException {
        checkIndex(columnIndex);
        delegate.updateObject(targetIndex, x, scaleOrLength);
    }

    @Override
    public void updateObject(int columnIndex, Object x) throws SQLException {
        checkIndex(columnIndex);
        delegate.updateObject(targetIndex, x);
    }

    @Override
    public void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException {
        checkLabel(columnLabel);
        delegate.updateObject(targetIndex, x, scaleOrLength);
    }

    @Override
    public void updateObject(String columnLabel, Object x) throws SQLException {
        checkLabel(columnLabel);
        delegate.updateObject(targetIndex, x);
    }

    @Override
    public void updateRef(int columnIndex, Ref x) throws SQLException {
        checkIndex(columnIndex);
        delegate.updateRef(targetIndex, x);
    }

    @Override
    public void updateRef(String columnLabel, Ref x) throws SQLException {
        checkLabel(columnLabel);
        delegate.updateRef(targetIndex, x);
    }

    @Override
    public void updateBlob(int columnIndex, Blob x) throws SQLException {
        checkIndex(columnIndex);
        delegate.updateBlob(targetIndex, x);
    }

    @Override
    public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
        checkIndex(columnIndex);
        delegate.updateBlob(targetIndex, inputStream);
    }

    @Override
    public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {
        checkIndex(columnIndex);
        delegate.updateBlob(targetIndex, inputStream, length);
    }

    @Override
    public void updateBlob(String columnLabel, Blob x) throws SQLException {
        checkLabel(columnLabel);
        delegate.updateBlob(targetIndex, x);
    }

    @Override
    public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
        checkLabel(columnLabel);
        delegate.updateBlob(targetIndex, inputStream);
    }

    @Override
    public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {
        checkLabel(columnLabel);
        delegate.updateBlob(targetIndex, inputStream, length);
    }

    @Override
    public void updateClob(int columnIndex, Clob x) throws SQLException {
        checkIndex(columnIndex);
        delegate.updateClob(targetIndex, x);
    }

    @Override
    public void updateClob(int columnIndex, Reader reader) throws SQLException {
        checkIndex(columnIndex);
        delegate.updateClob(targetIndex, reader);
    }

    @Override
    public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
        checkIndex(columnIndex);
        delegate.updateClob(targetIndex, reader, length);
    }

    @Override
    public void updateClob(String columnLabel, Clob x) throws SQLException {
        checkLabel(columnLabel);
        delegate.updateClob(targetIndex, x);
    }

    @Override
    public void updateClob(String columnLabel, Reader reader) throws SQLException {
        checkLabel(columnLabel);
        delegate.updateClob(targetIndex, reader);
    }

    @Override
    public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
        checkLabel(columnLabel);
        delegate.updateClob(targetIndex, reader, length);
    }

    @Override
    public void updateArray(int columnIndex, Array x) throws SQLException {
        checkIndex(columnIndex);
        delegate.updateArray(targetIndex, x);
    }

    @Override
    public void updateArray(String columnLabel, Array x) throws SQLException {
        checkLabel(columnLabel);
        delegate.updateArray(targetIndex, x);
    }

    @Override
    public void updateRowId(int columnIndex, RowId x) throws SQLException {
        checkIndex(columnIndex);
        delegate.updateRowId(targetIndex, x);
    }

    @Override
    public void updateRowId(String columnLabel, RowId x) throws SQLException {
        checkLabel(columnLabel);
        delegate.updateRowId(targetIndex, x);
    }

    @Override
    public void updateNString(int columnIndex, String nString) throws SQLException {
        checkIndex(columnIndex);
        delegate.updateNString(targetIndex, nString);
    }

    @Override
    public void updateNString(String columnLabel, String nString) throws SQLException {
        checkLabel(columnLabel);
        delegate.updateNString(targetIndex, nString);
    }

    @Override
    public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
        checkIndex(columnIndex);
        delegate.updateNClob(targetIndex, nClob);
    }

    @Override
    public void updateNClob(String columnLabel, NClob nClob) throws SQLException {
        checkLabel(columnLabel);
        delegate.updateNClob(targetIndex, nClob);
    }

    @Override
    public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
        checkIndex(columnIndex);
        delegate.updateSQLXML(targetIndex, xmlObject);
    }

    @Override
    public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {
        checkLabel(columnLabel);
        delegate.updateSQLXML(targetIndex, xmlObject);
    }

    @Override
    public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
        checkIndex(columnIndex);
        delegate.updateNCharacterStream(targetIndex, x, length);
    }

    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
        checkLabel(columnLabel);
        delegate.updateNCharacterStream(targetIndex, reader, length);
    }

    @Override
    public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {
        checkIndex(columnIndex);
        delegate.updateNClob(targetIndex, reader, length);
    }

    @Override
    public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
        checkLabel(columnLabel);
        delegate.updateNClob(targetIndex, reader, length);
    }

    @Override
    public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {
        checkIndex(columnIndex);
        delegate.updateNCharacterStream(targetIndex, x);
    }

    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {
        checkLabel(columnLabel);
        delegate.updateNCharacterStream(targetIndex, reader);
    }

    @Override
    public void updateNClob(int columnIndex, Reader reader) throws SQLException {
        checkIndex(columnIndex);
        delegate.updateNClob(targetIndex, reader);
    }

    @Override
    public void updateNClob(String columnLabel, Reader reader) throws SQLException {
        checkLabel(columnLabel);
        delegate.updateNClob(targetIndex, reader);
    }

    //// cursor accessors / mutators

    @Override
    public boolean next() throws SQLException {
        return delegate.next();
    }

    @Override
    public void close() throws SQLException {
        delegate.close();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return delegate.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException {
        delegate.clearWarnings();
    }

    @Override
    public String getCursorName() throws SQLException {
        return delegate.getCursorName();
    }

    @Override
    public boolean isBeforeFirst() throws SQLException {
        return delegate.isBeforeFirst();
    }

    @Override
    public boolean isAfterLast() throws SQLException {
        return delegate.isAfterLast();
    }

    @Override
    public boolean isFirst() throws SQLException {
        return delegate.isFirst();
    }

    @Override
    public boolean isLast() throws SQLException {
        return delegate.isLast();
    }

    @Override
    public void beforeFirst() throws SQLException {
        delegate.beforeFirst();
    }

    @Override
    public void afterLast() throws SQLException {
        delegate.afterLast();
    }

    @Override
    public boolean first() throws SQLException {
        return delegate.first();
    }

    @Override
    public boolean last() throws SQLException {
        return delegate.last();
    }

    @Override
    public int getRow() throws SQLException {
        return delegate.getRow();
    }

    @Override
    public boolean absolute(int row) throws SQLException {
        return delegate.absolute(row);
    }

    @Override
    public boolean relative(int rows) throws SQLException {
        return delegate.relative(rows);
    }

    @Override
    public boolean previous() throws SQLException {
        return delegate.previous();
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        delegate.setFetchDirection(direction);
    }

    @Override
    public int getFetchDirection() throws SQLException {
        return delegate.getFetchDirection();
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {
        delegate.setFetchSize(rows);
    }

    @Override
    public int getFetchSize() throws SQLException {
        return delegate.getFetchSize();
    }

    @Override
    public int getType() throws SQLException {
        return delegate.getType();
    }

    @Override
    public int getConcurrency() throws SQLException {
        return delegate.getConcurrency();
    }

    @Override
    public boolean rowUpdated() throws SQLException {
        return delegate.rowUpdated();
    }

    @Override
    public boolean rowInserted() throws SQLException {
        return delegate.rowInserted();
    }

    @Override
    public boolean rowDeleted() throws SQLException {
        return delegate.rowDeleted();
    }

    @Override
    public void insertRow() throws SQLException {
        delegate.insertRow();
    }

    @Override
    public void updateRow() throws SQLException {
        delegate.updateRow();
    }

    @Override
    public void deleteRow() throws SQLException {
        delegate.deleteRow();
    }

    @Override
    public void refreshRow() throws SQLException {
        delegate.refreshRow();
    }

    @Override
    public void cancelRowUpdates() throws SQLException {
        delegate.cancelRowUpdates();
    }

    @Override
    public void moveToInsertRow() throws SQLException {
        delegate.moveToInsertRow();
    }

    @Override
    public void moveToCurrentRow() throws SQLException {
        delegate.moveToCurrentRow();
    }

    @Override
    public Statement getStatement() throws SQLException {
        return delegate.getStatement();
    }

    @Override
    public int getHoldability() throws SQLException {
        return delegate.getHoldability();
    }

    @Override
    public boolean isClosed() throws SQLException {
        return delegate.isClosed();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return delegate.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return delegate.isWrapperFor(iface);
    }

    private class SingleColumnMetadata implements ResultSetMetaData {
        private final ResultSetMetaData delegate;

        public SingleColumnMetadata(ResultSetMetaData delegate) {
            this.delegate = delegate;
        }

        @Override
        public int getColumnCount() throws SQLException {
            return 1;
        }

        @Override
        public boolean isAutoIncrement(int column) throws SQLException {
            checkIndex(column);
            return delegate.isAutoIncrement(targetIndex);
        }

        @Override
        public boolean isCaseSensitive(int column) throws SQLException {
            checkIndex(column);
            return delegate.isCaseSensitive(targetIndex);
        }

        @Override
        public boolean isSearchable(int column) throws SQLException {
            checkIndex(column);
            return delegate.isSearchable(targetIndex);
        }

        @Override
        public boolean isCurrency(int column) throws SQLException {
            checkIndex(column);
            return delegate.isCurrency(targetIndex);
        }

        @Override
        public int isNullable(int column) throws SQLException {
            checkIndex(column);
            return delegate.isNullable(targetIndex);
        }

        @Override
        public boolean isSigned(int column) throws SQLException {
            checkIndex(column);
            return delegate.isSigned(targetIndex);
        }

        @Override
        public int getColumnDisplaySize(int column) throws SQLException {
            checkIndex(column);
            return delegate.getColumnDisplaySize(targetIndex);
        }

        @Override
        public String getColumnLabel(int column) throws SQLException {
            checkIndex(column);
            return targetLabel;
        }

        @Override
        public String getColumnName(int column) throws SQLException {
            checkIndex(column);
            return delegate.getColumnName(targetIndex);
        }

        @Override
        public String getSchemaName(int column) throws SQLException {
            checkIndex(column);
            return delegate.getSchemaName(targetIndex);
        }

        @Override
        public int getPrecision(int column) throws SQLException {
            checkIndex(column);
            return delegate.getPrecision(targetIndex);
        }

        @Override
        public int getScale(int column) throws SQLException {
            checkIndex(column);
            return delegate.getScale(targetIndex);
        }

        @Override
        public String getTableName(int column) throws SQLException {
            checkIndex(column);
            return delegate.getTableName(targetIndex);
        }

        @Override
        public String getCatalogName(int column) throws SQLException {
            checkIndex(column);
            return delegate.getCatalogName(targetIndex);
        }

        @Override
        public int getColumnType(int column) throws SQLException {
            checkIndex(column);
            return delegate.getColumnType(targetIndex);
        }

        @Override
        public String getColumnTypeName(int column) throws SQLException {
            checkIndex(column);
            return delegate.getColumnTypeName(targetIndex);
        }

        @Override
        public boolean isReadOnly(int column) throws SQLException {
            checkIndex(column);
            return delegate.isReadOnly(targetIndex);
        }

        @Override
        public boolean isWritable(int column) throws SQLException {
            checkIndex(column);
            return delegate.isWritable(targetIndex);
        }

        @Override
        public boolean isDefinitelyWritable(int column) throws SQLException {
            checkIndex(column);
            return delegate.isDefinitelyWritable(targetIndex);
        }

        @Override
        public String getColumnClassName(int column) throws SQLException {
            checkIndex(column);
            return delegate.getColumnClassName(targetIndex);
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            return delegate.unwrap(iface);
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            return delegate.isWrapperFor(iface);
        }
    }
}
