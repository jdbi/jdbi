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

/**
 * Interface for classes that hold configuration.
 *
 * @param <C> A "This" type. Should always be the configuration class.
 */
public interface JdbiConfig<C extends JdbiConfig<C>> {
    /**
     * @return a copy of this configuration object. Changes to the copy should not modify the original.
     */
    C createChild();
}
