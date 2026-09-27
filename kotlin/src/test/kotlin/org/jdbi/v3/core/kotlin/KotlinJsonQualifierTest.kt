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

package org.jdbi.v3.core.kotlin

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonQualifier
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import org.assertj.core.api.Assertions.assertThat
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.qualifier.Qualifier
import org.jdbi.v3.json.Json
import org.jdbi.v3.moshi.MoshiConfig
import org.jdbi.v3.moshi.MoshiPlugin
import org.jdbi.v3.testing.junit5.JdbiExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class KotlinJsonQualifierTest {

    @RegisterExtension
    @JvmField
    val h2Extension: JdbiExtension = JdbiExtension.h2().withPlugins(KotlinPlugin(), MoshiPlugin())

    private lateinit var handle: Handle

    @BeforeEach
    fun setup() {
        handle = h2Extension.sharedHandle
        handle.configure(MoshiConfig::class.java) { it.moshi = Moshi.Builder().add(ShoutingAdapter).build() }
        handle.execute("create table shouts (id int, shout varchar)")
    }

    @Test
    fun bindDataClassWithJsonQualifiedProperty() {
        handle.createUpdate("insert into shouts (id, shout) values (:id, :shout)")
            .bindKotlin(Shout(1, "hello"))
            .execute()

        assertThat(handle.select("select shout from shouts").mapTo<String>().one())
            .isEqualTo("\"HELLO\"")
    }

    @Test
    fun mapDataClassWithJsonQualifiedConstructorParam() {
        handle.execute("insert into shouts (id, shout) values (1, '\"HELLO\"')")

        assertThat(handle.select("select id, shout from shouts").mapTo<Shout>().one())
            .isEqualTo(Shout(1, "hello"))
    }

    data class Shout(val id: Int, @Json @Shouting val shout: String)

    @Retention(AnnotationRetention.RUNTIME)
    @JsonQualifier
    @Qualifier
    annotation class Shouting

    object ShoutingAdapter {
        @ToJson
        fun toJson(@Shouting value: String): String = value.uppercase()

        @FromJson
        @Shouting
        fun fromJson(value: String): String = value.lowercase()
    }
}
