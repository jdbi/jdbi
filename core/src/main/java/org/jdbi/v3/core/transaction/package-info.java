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
 * The <code>transaction</code> package implements the strategy
 * <code>Jdbi</code> uses to open and close transactions.  The default
 * instance simply sets the transaction property on the connection.
 * There is also a runner that runs <code>SERIALIZABLE</code> transactions
 * repeatedly until they succeed without transient serialization failures.
 * </p>
 */
package org.jdbi.v3.core.transaction;
