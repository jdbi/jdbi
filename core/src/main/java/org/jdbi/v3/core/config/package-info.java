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
 * The <code>config</code> classes define a configuration registry starting from
 * each <code>Jdbi</code> instance.  When a <code>Handle</code> or other configurable
 * object is created, it clones the parent's configuration at time of creation.
 * Modifying configuration will affect future created children but not existing
 * ones.  In general, it is preferable to configure <code>Jdbi</code> during
 * application initialization and then only configure specific statements
 * further individually if necessary.
 * </p>
 */
package org.jdbi.v3.core.config;
