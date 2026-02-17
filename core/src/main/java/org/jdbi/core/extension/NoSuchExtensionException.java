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
package org.jdbi.core.extension;

import org.jdbi.core.JdbiException;

import static java.lang.String.format;

/**
 * Thrown when no {@link ExtensionFactory} accepts a given extension type.
 */
public final class NoSuchExtensionException extends JdbiException {
    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_MESSAGE = "Extension not found: %s [Hint: maybe you need to register a plugin, like SqlObjectPlugin]";

    /**
     * Creates an instance with an extension type and the default message.
     *
     * @param extensionClass The extension type. Can be null.
     */
    public NoSuchExtensionException(Class<?> extensionClass) {
        super(formatMessage(extensionClass, DEFAULT_MESSAGE));
    }

    /**
     * Creates an instance with an extension type and a custom message.
     *
     * @param message The message format for the exception. Must contain exactly one <code>%s</code> placeholder for the extension class name.
     * @param extensionClass The extension type. Can be null.
     */
    public NoSuchExtensionException(Class<?> extensionClass, String message) {
        super(formatMessage(extensionClass, message));
    }

    /**
     * Creates an instance with an extension type and the default message.
     *
     * @param extensionClass The extension type. Can be null.
     * @param cause A throwable that caused this exception.
     */
    public NoSuchExtensionException(Class<?> extensionClass, Throwable cause) {
        super(formatMessage(extensionClass, DEFAULT_MESSAGE), cause);
    }

    /**
     * Creates an instance with an extension type and a custom message.
     *
     * @param message The message format for the exception. Must contain exactly one <code>%s</code> placeholder for the extension class name.
     * @param extensionClass The extension type. Can be null.
     * @param cause A throwable that caused this exception.
     */
    public NoSuchExtensionException(Class<?> extensionClass, String message, Throwable cause) {
        super(formatMessage(extensionClass, message), cause);
    }

    private static String formatMessage(Class<?> extensionClass, String formatString) {
        return format(formatString, extensionClass == null ? "<null>" : extensionClass.getSimpleName());
    }
}
