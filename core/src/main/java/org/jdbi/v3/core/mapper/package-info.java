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
/**
 * <p>
 * <code>mapper</code>s take the JDBC ResultSet and produce Java results.
 * ColumnMappers inspect a single result column, and RowMappers inspect the
 * entire result row to build a compound type.  Mappers are composeable and
 * often will feed results into the <code>collector</code> package to produce
 * the end result.
 * </p>
 */
package org.jdbi.v3.core.mapper;
