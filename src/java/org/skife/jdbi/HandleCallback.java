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

/**
 * Callback interface to be used with DBI instances a la
 *
 * <pre><code>
 * dbi.withHandle(new HandleCallback() {
 *      public void withHandle(Handle handle) {
 *          handle.execute("do-stuff");
 *          handle.execute("do-more-stuff");
 *      }
 * }
 * </code></pre>
 *
 * Guarantees resources will be closed appropriately
 */
public interface HandleCallback
{
    /**
     * Will be called by the dbi instance
     * 
     * @param handle Live instance
     * @throws Exception anything thrown will be re-thrown after reource cleanup
     */
    public void withHandle(Handle handle) throws Exception;
}
