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
package org.jdbi.v3.core.extension;

import org.jdbi.v3.core.JdbiException;
import org.jdbi.v3.meta.Beta;

import static java.lang.String.format;

/**
 * Marks that a specific extension could not be created.
 *
 * @since 3.38.0
 */
@Beta
public final class UnableToCreateExtensionException extends JdbiException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new instance with a message.
     * @param format A {@link String#format} format string
     * @param args Arguments for the format string
     */
    public UnableToCreateExtensionException(String format, Object... args) {
        super(format(format, args));
    }

    /**
     * Constructs a new instance with an exception and a message.
     * @param throwable A throwable
     * @param format A {@link String#format} format string
     * @param args Arguments for the format string
     */
    public UnableToCreateExtensionException(Throwable throwable, String format, Object... args) {
        super(format(format, args), throwable);
    }
}
