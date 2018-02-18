/*
 * Copyright 2017-2018 the original author or authors.
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

package com.nebhale.r2dbc.postgresql.type;

import java.util.Arrays;

/**
 * Object IDs for well know PostgreSQL data types.
 */
public enum PostgresqlObjectId {

    /**
     * The bit object id.
     */
    BIT(1560),

    /**
     * The bit array object id.
     */
    BIT_ARRAY(1561),

    /**
     * The bool object id.
     */
    BOOL(16),

    /**
     * The bool array object id.
     */
    BOOL_ARRAY(1000),

    /**
     * The box object id.
     */
    BOX(603),

    /**
     * The bpchar object id.
     */
    BPCHAR(1042),

    /**
     * The bpchar array object id.
     */
    BPCHAR_ARRAY(1014),

    /**
     * The bytea object id.
     */
    BYTEA(17),

    /**
     * They bytea array object id.
     */
    BYTEA_ARRAY(1001),

    /**
     * The char object id.
     */
    CHAR(18),

    /**
     * The char array object id.
     */
    CHAR_ARRAY(1002),

    /**
     * The date object id.
     */
    DATE(1082),

    /**
     * The date array object id.
     */
    DATE_ARRAY(1182),

    /**
     * The float4 object id.
     */
    FLOAT4(700),

    /**
     * The float4 array object id.
     */
    FLOAT4_ARRAY(1021),

    /**
     * The float8 object id.
     */
    FLOAT8(701),

    /**
     * The float8 array object id.
     */
    FLOAT8_ARRAY(1022),

    /**
     * The int2 object id.
     */
    INT2(21),

    /**
     * The int2 array object id.
     */
    INT2_ARRAY(1005),

    /**
     * The int4 object id.
     */
    INT4(23),

    /**
     * The int4 array object id.
     */
    INT4_ARRAY(1007),

    /**
     * The int8 object id.
     */
    INT8(20),

    /**
     * The int8 array object id.
     */
    INT8_ARRAY(1016),

    /**
     * The interval object id.
     */
    INTERVAL(1186),

    /**
     * The interval array object id.
     */
    INTERVAL_ARRAY(1187),

    /**
     * The JSON object id.
     */
    JSON(114),

    /**
     * The JSON array object id.
     */
    JSON_ARRAY(199),

    /**
     * The JSONB array object id.
     */
    JSONB_ARRAY(3807),

    /**
     * The money object id.
     */
    MONEY(790),

    /**
     * The money array object id.
     */
    MONEY_ARRAY(791),

    /**
     * The name object id.
     */
    NAME(19),

    /**
     * The name array object id.
     */
    NAME_ARRAY(1003),

    /**
     * The numberic object id.
     */
    NUMERIC(1700),

    /**
     * The numeric array object id.
     */
    NUMERIC_ARRAY(1231),

    /**
     * The oid object id.
     */
    OID(26),

    /**
     * The oid array object id.
     */
    OID_ARRAY(1028),

    /**
     * The point object id.
     */
    POINT(600),

    /**
     * The point array object id.
     */
    POINT_ARRAY(1017),

    /**
     * The ref cursor object id.
     */
    REF_CURSOR(1790),

    /**
     * The ref cursor array object id.
     */
    REF_CURSOR_ARRAY(2201),

    /**
     * The text object id.
     */
    TEXT(25),

    /**
     * The text array object id.
     */
    TEXT_ARRAY(1009),

    /**
     * The time object id.
     */
    TIME(1083),

    /**
     * The time array object id.
     */
    TIME_ARRAY(1183),

    /**
     * The timestamp object id.
     */
    TIMESTAMP(1114),

    /**
     * The timestamp array object id.
     */
    TIMESTAMP_ARRAY(1115),

    /**
     * The timestamptz object id.
     */
    TIMESTAMPTZ(1184),

    /**
     * The timestamptz array object id.
     */
    TIMESTAMPTZ_ARRAY(1185),

    /**
     * The timetz object id.
     */
    TIMETZ(1266),

    /**
     * The timetz array object id.
     */
    TIMETZ_ARRAY(1270),

    /**
     * The unspecified object id.
     */
    UNSPECIFIED(2),

    /**
     * The UUID object id.
     */
    UUID(2950),

    /**
     * The UUID array object id.
     */
    UUID_ARRAY(2951),

    /**
     * The varbit object id.
     */
    VARBIT(1562),

    /**
     * The varbit array object id.
     */
    VARBIT_ARRAY(1563),

    /**
     * The varchar object id.
     */
    VARCHAR(1043),

    /**
     * The varchar array object id.
     */
    VARCHAR_ARRAY(1015),

    /**
     * The void object id.
     */
    VOID(2278),

    /**
     * The XML object id.
     */
    XML(142),

    /**
     * The XML Array object id.
     */
    XML_ARRAY(143);

    private final int objectId;

    PostgresqlObjectId(int objectId) {
        this.objectId = objectId;
    }

    /**
     * Returns the {@link PostgresqlObjectId} matching a given object id.
     *
     * @param objectId the object id to match
     * @return the {@link PostgresqlObjectId} matching a given object id
     * @throws IllegalArgumentException if {@code objectId} isn't a valid object id
     */
    public static PostgresqlObjectId valueOf(int objectId) {
        return Arrays.stream(values())
            .filter(type -> type.objectId == objectId)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(String.format("%d is not a valid object id", objectId)));
    }

    /**
     * Returns the object id represented by each return value.
     *
     * @return the object id represented by each return value
     */
    public int getObjectId() {
        return this.objectId;
    }

}
