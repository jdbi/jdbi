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

import com.nebhale.r2dbc.postgresql.message.backend.BackendMessage;
import com.nebhale.r2dbc.postgresql.message.backend.ErrorResponse;
import com.nebhale.r2dbc.postgresql.message.backend.Field;
import com.nebhale.r2dbc.postgresql.message.backend.Field.FieldType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.nebhale.r2dbc.postgresql.message.backend.Field.FieldType.CODE;
import static com.nebhale.r2dbc.postgresql.message.backend.Field.FieldType.COLUMN_NAME;
import static com.nebhale.r2dbc.postgresql.message.backend.Field.FieldType.CONSTRAINT_NAME;
import static com.nebhale.r2dbc.postgresql.message.backend.Field.FieldType.DATA_TYPE_NAME;
import static com.nebhale.r2dbc.postgresql.message.backend.Field.FieldType.DETAIL;
import static com.nebhale.r2dbc.postgresql.message.backend.Field.FieldType.FILE;
import static com.nebhale.r2dbc.postgresql.message.backend.Field.FieldType.HINT;
import static com.nebhale.r2dbc.postgresql.message.backend.Field.FieldType.INTERNAL_POSITION;
import static com.nebhale.r2dbc.postgresql.message.backend.Field.FieldType.INTERNAL_QUERY;
import static com.nebhale.r2dbc.postgresql.message.backend.Field.FieldType.LINE;
import static com.nebhale.r2dbc.postgresql.message.backend.Field.FieldType.MESSAGE;
import static com.nebhale.r2dbc.postgresql.message.backend.Field.FieldType.POSITION;
import static com.nebhale.r2dbc.postgresql.message.backend.Field.FieldType.ROUTINE;
import static com.nebhale.r2dbc.postgresql.message.backend.Field.FieldType.SCHEMA_NAME;
import static com.nebhale.r2dbc.postgresql.message.backend.Field.FieldType.SEVERITY_LOCALIZED;
import static com.nebhale.r2dbc.postgresql.message.backend.Field.FieldType.SEVERITY_NON_LOCALIZED;
import static com.nebhale.r2dbc.postgresql.message.backend.Field.FieldType.TABLE_NAME;
import static com.nebhale.r2dbc.postgresql.message.backend.Field.FieldType.WHERE;
import static java.util.Objects.requireNonNull;

/**
 * An exception that represents a server error.  This exception is a direct translation of the {@link ErrorResponse} message.
 */
public final class PostgresqlServerErrorException extends RuntimeException {

    private final String code;

    private final String columnName;

    private final String constraintName;

    private final String dataTypeName;

    private final String detail;

    private final String file;

    private final String hint;

    private final String internalPosition;

    private final String internalQuery;

    private final String line;

    private final String message;

    private final String position;

    private final String routine;

    private final String schemaName;

    private final String severityLocalized;

    private final String severityNonLocalized;

    private final String tableName;

    private final String where;


    /**
     * Creates a new exception.
     *
     * @param fields the fields to be used to populate the exception
     * @throws NullPointerException if {@code fields} is {@code null}
     */
    public PostgresqlServerErrorException(List<Field> fields) {
        requireNonNull(fields, "fields must not be null");

        Map<FieldType, String> map = fields.stream()
            .collect(Collectors.toMap(Field::getType, Field::getValue));

        this.code = map.get(CODE);
        this.columnName = map.get(COLUMN_NAME);
        this.constraintName = map.get(CONSTRAINT_NAME);
        this.dataTypeName = map.get(DATA_TYPE_NAME);
        this.detail = map.get(DETAIL);
        this.file = map.get(FILE);
        this.hint = map.get(HINT);
        this.internalPosition = map.get(INTERNAL_POSITION);
        this.internalQuery = map.get(INTERNAL_QUERY);
        this.line = map.get(LINE);
        this.message = map.get(MESSAGE);
        this.position = map.get(POSITION);
        this.routine = map.get(ROUTINE);
        this.schemaName = map.get(SCHEMA_NAME);
        this.severityLocalized = map.get(SEVERITY_LOCALIZED);
        this.severityNonLocalized = map.get(SEVERITY_NON_LOCALIZED);
        this.tableName = map.get(TABLE_NAME);
        this.where = map.get(WHERE);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PostgresqlServerErrorException that = (PostgresqlServerErrorException) o;
        return Objects.equals(this.code, that.code) &&
            Objects.equals(this.columnName, that.columnName) &&
            Objects.equals(this.constraintName, that.constraintName) &&
            Objects.equals(this.dataTypeName, that.dataTypeName) &&
            Objects.equals(this.detail, that.detail) &&
            Objects.equals(this.file, that.file) &&
            Objects.equals(this.hint, that.hint) &&
            Objects.equals(this.internalPosition, that.internalPosition) &&
            Objects.equals(this.internalQuery, that.internalQuery) &&
            Objects.equals(this.line, that.line) &&
            Objects.equals(this.message, that.message) &&
            Objects.equals(this.position, that.position) &&
            Objects.equals(this.routine, that.routine) &&
            Objects.equals(this.schemaName, that.schemaName) &&
            Objects.equals(this.severityLocalized, that.severityLocalized) &&
            Objects.equals(this.severityNonLocalized, that.severityNonLocalized) &&
            Objects.equals(this.tableName, that.tableName) &&
            Objects.equals(this.where, that.where);
    }

    /**
     * Returns the value of the {@link FieldType#CODE} field.
     *
     * @return the value of the {@link FieldType#CODE} field
     */
    public String getCode() {
        return this.code;
    }

    /**
     * Returns the value of the {@link FieldType#COLUMN_NAME} field.
     *
     * @return the value of the {@link FieldType#COLUMN_NAME} field
     */
    public String getColumnName() {
        return this.columnName;
    }

    /**
     * Returns the value of the {@link FieldType#CONSTRAINT_NAME} field.
     *
     * @return the value of the {@link FieldType#CONSTRAINT_NAME} field
     */
    public String getConstraintName() {
        return this.constraintName;
    }

    /**
     * Returns the value of the {@link FieldType#DATA_TYPE_NAME} field.
     *
     * @return the value of the {@link FieldType#DATA_TYPE_NAME} field
     */
    public String getDataTypeName() {
        return this.dataTypeName;
    }

    /**
     * Returns the value of the {@link FieldType#DETAIL} field.
     *
     * @return the value of the {@link FieldType#DETAIL} field
     */
    public String getDetail() {
        return this.detail;
    }

    /**
     * Returns the value of the {@link FieldType#FILE} field.
     *
     * @return the value of the {@link FieldType#FILE} field
     */
    public String getFile() {
        return this.file;
    }

    /**
     * Returns the value of the {@link FieldType#HINT} field.
     *
     * @return the value of the {@link FieldType#HINT} field
     */
    public String getHint() {
        return this.hint;
    }

    /**
     * Returns the value of the {@link FieldType#INTERNAL_POSITION} field.
     *
     * @return the value of the {@link FieldType#INTERNAL_POSITION} field
     */
    public String getInternalPosition() {
        return this.internalPosition;
    }

    /**
     * Returns the value of the {@link FieldType#INTERNAL_QUERY} field.
     *
     * @return the value of the {@link FieldType#INTERNAL_QUERY} field
     */
    public String getInternalQuery() {
        return this.internalQuery;
    }

    /**
     * Returns the value of the {@link FieldType#LINE} field.
     *
     * @return the value of the {@link FieldType#LINE} field
     */
    public String getLine() {
        return this.line;
    }

    /**
     * Returns the value of the {@link FieldType#MESSAGE} field.
     *
     * @return the value of the {@link FieldType#MESSAGE} field
     */
    @Override
    public String getMessage() {
        return this.message;
    }

    /**
     * Returns the value of the {@link FieldType#POSITION} field.
     *
     * @return the value of the {@link FieldType#POSITION} field
     */
    public String getPosition() {
        return this.position;
    }

    /**
     * Returns the value of the {@link FieldType#ROUTINE} field.
     *
     * @return the value of the {@link FieldType#ROUTINE} field
     */
    public String getRoutine() {
        return this.routine;
    }

    /**
     * Returns the value of the {@link FieldType#SCHEMA_NAME} field.
     *
     * @return the value of the {@link FieldType#SCHEMA_NAME} field
     */
    public String getSchemaName() {
        return this.schemaName;
    }

    /**
     * Returns the value of the {@link FieldType#SEVERITY_LOCALIZED} field.
     *
     * @return the value of the {@link FieldType#SEVERITY_LOCALIZED} field
     */
    public String getSeverityLocalized() {
        return this.severityLocalized;
    }

    /**
     * Returns the value of the {@link FieldType#SEVERITY_NON_LOCALIZED} field.
     *
     * @return the value of the {@link FieldType#SEVERITY_NON_LOCALIZED} field
     */
    public String getSeverityNonLocalized() {
        return this.severityNonLocalized;
    }

    /**
     * Returns the value of the {@link FieldType#TABLE_NAME} field.
     *
     * @return the value of the {@link FieldType#TABLE_NAME} field
     */
    public String getTableName() {
        return this.tableName;
    }

    /**
     * Returns the value of the {@link FieldType#WHERE} field.
     *
     * @return the value of the {@link FieldType#WHERE} field
     */
    public String getWhere() {
        return this.where;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.code, this.columnName, this.constraintName, this.dataTypeName, this.detail, this.file, this.hint, this.internalPosition, this.internalQuery, this.line,
            this.message, this.position, this.routine, this.schemaName, this.severityLocalized, this.severityNonLocalized, this.tableName, this.where);
    }

    @Override
    public String toString() {
        return "PostgresqlServerErrorException{" +
            "code='" + this.code + '\'' +
            ", columnName='" + this.columnName + '\'' +
            ", constraintName='" + this.constraintName + '\'' +
            ", dataTypeName='" + this.dataTypeName + '\'' +
            ", detail='" + this.detail + '\'' +
            ", file='" + this.file + '\'' +
            ", hint='" + this.hint + '\'' +
            ", internalPosition='" + this.internalPosition + '\'' +
            ", internalQuery='" + this.internalQuery + '\'' +
            ", line='" + this.line + '\'' +
            ", message='" + this.message + '\'' +
            ", position='" + this.position + '\'' +
            ", routine='" + this.routine + '\'' +
            ", schemaName='" + this.schemaName + '\'' +
            ", severityLocalized='" + this.severityLocalized + '\'' +
            ", severityNonLocalized='" + this.severityNonLocalized + '\'' +
            ", tableName='" + this.tableName + '\'' +
            ", where='" + this.where + '\'' +
            "} " + super.toString();
    }

    static void handleErrorResponse(BackendMessage message, SynchronousSink<BackendMessage> sink) {
        if (message instanceof ErrorResponse) {
            sink.error(PostgresqlServerErrorException.toException((ErrorResponse) message));
        } else {
            sink.next(message);
        }
    }

    private static PostgresqlServerErrorException toException(ErrorResponse errorResponse) {
        requireNonNull(errorResponse, "errorResponse must not be null");

        return new PostgresqlServerErrorException(errorResponse.getFields());
    }

}
