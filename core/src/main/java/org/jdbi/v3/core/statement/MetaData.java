/*
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
package org.jdbi.v3.core.statement;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Supplier;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.result.ResultBearing;
import org.jdbi.v3.core.result.ResultSetScanner;

/**
 * Access to Database Metadata.
 */
public final class MetaData extends BaseStatement<MetaData> implements ResultBearing {

    private final MetaDataValueProvider<?> metaDataFunction;

    public MetaData(Handle handle, MetaDataValueProvider<?> metaDataFunction) {
        super(handle);
        this.metaDataFunction = metaDataFunction;
    }

    @Override
    public <R> R scanResultSet(ResultSetScanner<R> mapper) {
        return ResultBearing.of(getResultSetSupplier(), getContext())
            .scanResultSet(mapper);
    }

    @SuppressWarnings("TypeParameterUnusedInFormals")
    public <R> R execute() {
        try {
            Connection connection = getHandle().getConnection();
            return (R) metaDataFunction.provideValue(connection.getMetaData());
        } catch (SQLException e) {
            throw new UnableToRetrieveMetaDataException(e, getContext());
        }
    }

    @SuppressWarnings("UnnecessaryLambda") // factored out for readablity
    private Supplier<ResultSet> getResultSetSupplier() {
        return () -> {
            ResultSet rs = execute();
            getContext().addCleanable(rs::close);
            return rs;
        };
    }

    @FunctionalInterface
    public interface MetaDataValueProvider<T> {

        T provideValue(DatabaseMetaData databaseMetaData) throws SQLException;
    }

    @FunctionalInterface
    public interface MetaDataResultSetProvider extends MetaDataValueProvider<ResultSet> {

        @Override
        ResultSet provideValue(DatabaseMetaData databaseMetaData) throws SQLException;
    }
}
