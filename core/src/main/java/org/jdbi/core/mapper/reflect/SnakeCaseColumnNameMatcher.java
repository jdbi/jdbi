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
package org.jdbi.core.mapper.reflect;

/**
 * Matches snake case column names to java camel case names, ignoring case. This matcher is prefix aware and will
 * try multiple strategies to match a case name to a java name. Java names can be prefixed and separated with "."
 * <p>
 * <tt>foo_bar_baz</tt> can be mapped to <tt>fooBarBaz, foo.barBaz or foo.bar.baz</tt>
 *
 */
public class SnakeCaseColumnNameMatcher extends AbstractSeparatorCharColumnNameMatcher {

    public SnakeCaseColumnNameMatcher() {
        super('_');
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
