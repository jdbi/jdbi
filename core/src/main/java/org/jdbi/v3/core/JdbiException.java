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

/**
 * Base unchecked exception for exceptions thrown from jdbi.
 */
public abstract class JdbiException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * @param message the exception message
     * @param cause the optional cause
     */
    public JdbiException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param cause the cause of this exception
     */
    public JdbiException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new runtime exception with the specified detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public JdbiException(String message) {
        super(message);
    }
}
