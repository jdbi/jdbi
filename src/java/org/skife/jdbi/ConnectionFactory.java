/* Copyright 2004-2005 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.skife.jdbi;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Used when JDBC connections need to be obtained in an unusual manner,
 * such as from a proprietary o/r mapping interface.
 */
public interface ConnectionFactory
{
    /**
     * Must return a usable JDBC Connection instance, will be called
     * to obtain a Connection for each Handle opened
     */
    Connection getConnection() throws SQLException;
}
