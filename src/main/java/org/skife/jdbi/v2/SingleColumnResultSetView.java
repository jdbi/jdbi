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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * A ResultSet wrapper which exposes a single column from an underlying ResultSet to the caller.
 */
public final class SingleColumnResultSetView {
    private SingleColumnResultSetView() {
        // no instances
    }

    public static ResultSet newInstance(ResultSet delegate, int columnIndex) throws SQLException {
        return (ResultSet) Proxy.newProxyInstance(
                delegate.getClass().getClassLoader(),
                new Class[]{ResultSet.class},
                new ResultSetProxy(delegate, columnIndex));
    }

    static void checkIndex(int index) throws SQLException {
        if (index != 1) {
            throw new SQLException("Column index " + index + " out of range.");
        }
    }

    private static class ResultSetProxy implements InvocationHandler {
        private final ResultSet delegate;
        private final int columnIndex;
        private final String columnLabel;
        private final ResultSetMetaData metaData;

        public ResultSetProxy(ResultSet delegate, int columnIndex) throws SQLException {
            this.delegate = delegate;
            this.columnIndex = columnIndex;
            this.columnLabel = delegate.getMetaData().getColumnLabel(columnIndex);
            this.metaData = (ResultSetMetaData) Proxy.newProxyInstance(
                    delegate.getClass().getClassLoader(),
                    new Class[]{ResultSetMetaData.class},
                    new ResultSetMetaDataProxy(delegate.getMetaData(), columnIndex));
        }

        void checkLabel(String label) throws SQLException {
            if (!columnLabel.equals(label)) {
                throw new SQLException("Column label " + label + " not found.");
            }
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            if ("getMetaData".equals(methodName) && args == null) {
                return metaData;
            }
            else if ("findColumn".equals(methodName) && args != null && args.length == 1) {
                checkLabel((String) args[0]);
                return 1;
            }
            else if (args != null && (methodName.startsWith("get") || methodName.startsWith("update"))) {
                if (args[0] instanceof Integer) {
                    checkIndex((Integer) args[0]);
                    args[0] = columnIndex;
                }
                else if (args[0] instanceof String) {
                    checkLabel((String) args[0]);
                }
            }
            return method.invoke(delegate, args);
        }
    }

    private static class ResultSetMetaDataProxy implements InvocationHandler {
        private final ResultSetMetaData delegate;
        private final int columnIndex;

        ResultSetMetaDataProxy(ResultSetMetaData delegate, int columnIndex) {
            this.delegate = delegate;
            this.columnIndex = columnIndex;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("getColumnCount".equals(method.getName()) && args == null) {
                return 1;
            }
            else if (args != null && args[0] instanceof Integer) {
                checkIndex((Integer) args[0]);
                args[0] = columnIndex;
            }
            return method.invoke(delegate, args);
        }
    }
}
