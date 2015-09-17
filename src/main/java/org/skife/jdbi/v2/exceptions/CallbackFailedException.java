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
package org.skife.jdbi.v2.exceptions;

/**
 * Exception used to indicate an exception thrown during a provided callback. The wrapped
 * throwable will be the client library thrown checked exception.
 */
public class CallbackFailedException extends DBIException
{
    public CallbackFailedException(String string, Throwable throwable)
    {
        super(string, throwable);
    }

    public CallbackFailedException(Throwable cause)
    {
        super(cause);
    }
}
