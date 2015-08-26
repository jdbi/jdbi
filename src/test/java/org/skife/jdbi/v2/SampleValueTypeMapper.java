package org.skife.jdbi.v2;

import org.skife.jdbi.v2.util.TypedMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

class SampleValueTypeMapper extends TypedMapper<SampleValueType> {
    public SampleValueTypeMapper() {}

    @Override
    protected SampleValueType extractByName(ResultSet r, String name) throws SQLException {
        return SampleValueType.valueOf(r.getString(name));
    }

    @Override
    protected SampleValueType extractByIndex(ResultSet r, int index) throws SQLException {
        return SampleValueType.valueOf(r.getString(index));
    }
}
