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
 * Provides asynchronous execution of Jdbi operations.
 * The {@link org.jdbi.v3.core.async.JdbiExecutor} wraps a
 * {@link org.jdbi.v3.core.Jdbi} instance and an {@link java.util.concurrent.Executor}
 * to run callbacks asynchronously, returning
 * {@link java.util.concurrent.CompletionStage} results.
 * </p>
 */
package org.jdbi.v3.core.async;
