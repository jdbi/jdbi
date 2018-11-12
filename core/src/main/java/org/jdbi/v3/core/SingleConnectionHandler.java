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
package org.jdbi.v3.core;

import java.sql.Connection;

/**
 * Implementation of Connection handler, which works on single connection. close operation is ignored.
 *
 * @author Dmitry Tsydzik
 * @since 11/12/18
 */
public class SingleConnectionHandler implements ConnectionHandler {

    private final Connection connection;

    public SingleConnectionHandler(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public void releaseConnection(Connection connection) {}
}
