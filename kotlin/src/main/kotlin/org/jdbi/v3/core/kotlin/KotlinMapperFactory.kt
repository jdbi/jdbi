/**
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
package org.jdbi.v3.core.kotlin

import org.jdbi.v3.core.config.ConfigRegistry
import org.jdbi.v3.core.generic.GenericTypes.getErasedType
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.mapper.RowMapperFactory
import java.lang.reflect.Type
import java.util.*

class KotlinMapperFactory : RowMapperFactory {

    override fun build(type: Type, config: ConfigRegistry): Optional<RowMapper<*>> {
        val erasedType = getErasedType(type)

        //TODO: Validate if we should only handle 'data' classes with the Kotlin mapper
        // Switching this might cause issues for users, might be better to do it for a major release
        // See https://github.com/jdbi/jdbi/issues/1218 for more info
        return if (erasedType.isKotlinClass() && !erasedType.isEnum) {
            Optional.of(KotlinMapper(erasedType))
        } else {
            Optional.empty()
        }
    }
}
