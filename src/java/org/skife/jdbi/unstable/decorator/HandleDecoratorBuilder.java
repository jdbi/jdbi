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
package org.skife.jdbi.unstable.decorator;

import org.skife.jdbi.Handle;
import org.skife.jdbi.IDBI;
import org.skife.jdbi.unstable.Unstable;

/**
 * Used to decorate <code>Handle</code> instances obtained from a DBI instance.
 * The builder will be instantiated once per DBI, and the decorate(..) called
 * for each handle opened.
 */
public interface HandleDecoratorBuilder extends Unstable
{
    /**
     * Called when a handle is created
     *
     * @param dbi The <code>IDBI</code> instance the handle is created from
     * @param base the <code>Handle</code> being decorated
     * @return decorated handle
     */
    Handle decorate(IDBI dbi, Handle base);
}
