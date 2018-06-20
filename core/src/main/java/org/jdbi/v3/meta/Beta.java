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
package org.jdbi.v3.meta;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;

/**
 * Signifies that a public API (public class, method or field) is subject to incompatible changes, or even removal, in a future release. An API bearing this annotation is exempt from any compatibility guarantees made by its containing library.
 *
 * Note that the presence of this annotation implies nothing about the quality or performance of the API in question, only the fact that it is not "API-frozen."
 *
 * It is generally safe for applications to depend on beta APIs, at the cost of some extra work during upgrades. However it is generally inadvisable for libraries (which get included on users' CLASSPATHs, outside the library developers' control) to do so.
 *
 * @see <a href="https://google.github.io/guava/releases/15.0/api/docs/com/google/common/annotations/Beta.html">Courtesy of Kevin Bourrillion's @Beta at Guava</a>
 */
@Documented
@Inherited
@Target({TYPE, FIELD, METHOD, PARAMETER, CONSTRUCTOR, ANNOTATION_TYPE})
public @interface Beta {}
