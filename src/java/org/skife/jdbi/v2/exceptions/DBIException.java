/* Copyright 2004-2006 Brian McCallister
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
package org.skife.jdbi.v2.exceptions;

/**
 * Base runtime exception for exceptions thrown from jDBI
 */
public abstract class DBIException extends RuntimeException
{
    public DBIException(String string, Throwable throwable)
    {
        super(string, throwable);
    }

    public DBIException(Throwable cause)
    {
        super(cause);
    }
}
