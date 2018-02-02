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

package com.nebhale.r2dbc.postgresql;

import java.util.Arrays;

/**
 * Object IDs for well know PostgreSQL data types.
 */
public enum PostgresqlObjectId {

    BIT(1560),
    BIT_ARRAY(1561),
    BOOL(16),
    BOOL_ARRAY(1000),
    BOX(603),
    BPCHAR(1042),
    BPCHAR_ARRAY(1014),
    BYTEA(17),
    BYTEA_ARRAY(1001),
    CHAR(18),
    CHAR_ARRAY(1002),
    DATE(1082),
    DATE_ARRAY(1182),
    FLOAT4(700),
    FLOAT4_ARRAY(1021),
    FLOAT8(701),
    FLOAT8_ARRAY(1022),
    INT2(21),
    INT2_ARRAY(1005),
    INT4(23),
    INT4_ARRAY(1007),
    INT8(20),
    INT8_ARRAY(1016),
    INTERVAL(1186),
    INTERVAL_ARRAY(1187),
    JSON(114),
    JSON_ARRAY(199),
    JSONB_ARRAY(3807),
    MONEY(790),
    MONEY_ARRAY(791),
    NAME(19),
    NAME_ARRAY(1003),
    NUMERIC(1700),
    NUMERIC_ARRAY(1231),
    OID(26),
    OID_ARRAY(1028),
    POINT(600),
    POINT_ARRAY(1017),
    REF_CURSOR(1790),
    REF_CURSOR_ARRAY(2201),
    TEXT(25),
    TEXT_ARRAY(1009),
    TIME(1083),
    TIME_ARRAY(1183),
    TIMESTAMP(1114),
    TIMESTAMP_ARRAY(1115),
    TIMESTAMPTZ(1184),
    TIMESTAMPTZ_ARRAY(1185),
    TIMETZ(1266),
    TIMETZ_ARRAY(1270),
    UNSPECIFIED(2),
    UUID(2950),
    UUID_ARRAY(2951),
    VARBIT(1562),
    VARBIT_ARRAY(1563),
    VARCHAR(1043),
    VARCHAR_ARRAY(1015),
    VOID(2278),
    XML(142),
    XML_ARRAY(143);

    private final int objectId;

    PostgresqlObjectId(int objectId) {
        this.objectId = objectId;
    }

    /**
     * Returns the Object ID represented by each return value.
     *
     * @return the Object ID represented by each return value
     */
    public int getObjectId() {
        return this.objectId;
    }

    static PostgresqlObjectId valueOf(int i) {
        return Arrays.stream(values())
            .filter(type -> type.objectId == i)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(String.format("%d is not a valid object id", i)));
    }

}
