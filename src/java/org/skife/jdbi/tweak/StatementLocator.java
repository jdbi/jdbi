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
package org.skife.jdbi.tweak;

/**
 * Used to search for named statements
 */
public interface StatementLocator
{
    /**
     * Will be passed the name of a statement to locate. Must return valid SQL (with
     * the option of including named parameters jdbi style).
     *
     * @param key name of the statement
     * @return raw SQL statement (may include named params)
     */
    public String load(String key);
}
