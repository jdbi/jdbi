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
package org.jdbi.core.kotlin

import org.jdbi.core.interceptor.JdbiInterceptionChain
import org.jdbi.core.interceptor.JdbiInterceptor
import org.jdbi.core.mapper.RowMapper
import org.jdbi.core.mapper.RowMapperFactory

internal class KotlinRowMapperInterceptor : JdbiInterceptor<RowMapper<*>, RowMapperFactory> {

    override fun intercept(source: RowMapper<*>?, chain: JdbiInterceptionChain<RowMapperFactory>): RowMapperFactory = if (source is KotlinMapper) {
        RowMapperFactory.of(source.kClass.java, source)
    } else {
        chain.next()
    }
}
