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
module org.jdbi.v3.sqlobject {

	exports org.jdbi.v3.sqlobject;
	exports org.jdbi.v3.sqlobject.config;
	exports org.jdbi.v3.sqlobject.config.internal;
	exports org.jdbi.v3.sqlobject.customizer;
	exports org.jdbi.v3.sqlobject.customizer.internal;
	exports org.jdbi.v3.sqlobject.internal;
	exports org.jdbi.v3.sqlobject.locator;
	exports org.jdbi.v3.sqlobject.locator.internal;
	exports org.jdbi.v3.sqlobject.statement;
	exports org.jdbi.v3.sqlobject.statement.internal;
	exports org.jdbi.v3.sqlobject.transaction;
	exports org.jdbi.v3.sqlobject.transaction.internal;

    requires transitive org.jdbi.v3.core;
}
