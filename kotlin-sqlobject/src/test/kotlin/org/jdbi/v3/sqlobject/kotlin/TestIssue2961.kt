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
package org.jdbi.v3.sqlobject.kotlin

import org.assertj.core.api.Assertions.assertThat
import org.jdbi.v3.core.result.RowReducer
import org.jdbi.v3.core.result.RowView
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.UseRowReducer
import org.jdbi.v3.testing.junit5.JdbiExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.util.stream.Stream

class TestIssue2961 {
    @RegisterExtension
    @JvmField
    val h2Extension: JdbiExtension = JdbiExtension.h2()
        .withPlugin(KotlinSqlObjectPlugin())

    data class User(val id: Int, val name: String)

    data class Order(val id: Int, val item: String)

    data class UserOrder(val user: User, val order: Order)

    data class Transfer(val from: User, val to: User)

    interface MethodLevelDao {
        // The unprefixed id and name columns catch a fallback to the unprefixed KotlinMapperFactory mapper.
        @SqlQuery("select 1 as u_id, 'alice' as u_name, 10 as o_id, 'widget' as o_item, 99 as id, 'decoy' as name")
        @RegisterKotlinMapper(User::class, prefix = "u_")
        @RegisterKotlinMapper(Order::class, prefix = "o_")
        @UseRowReducer(UserOrderReducer::class)
        fun userOrders(): List<UserOrder>

        @SqlQuery("select 1 as a_id, 'alice' as a_name, 2 as b_id, 'bob' as b_name, 99 as id, 'decoy' as name")
        @RegisterKotlinMapper(User::class, prefix = "a_")
        @RegisterKotlinMapper(User::class, prefix = "b_")
        @UseRowReducer(TransferReducer::class)
        fun transfers(): List<Transfer>
    }

    @RegisterKotlinMapper(User::class, prefix = "u_")
    @RegisterKotlinMapper(Order::class, prefix = "o_")
    interface TypeLevelDao {
        @SqlQuery("select 1 as u_id, 'alice' as u_name, 10 as o_id, 'widget' as o_item, 99 as id, 'decoy' as name")
        @UseRowReducer(UserOrderReducer::class)
        fun userOrders(): List<UserOrder>
    }

    class UserOrderReducer : ListReducer<UserOrder>() {
        override fun map(rowView: RowView) = UserOrder(rowView.getRow(User::class.java), rowView.getRow(Order::class.java))
    }

    class TransferReducer : ListReducer<Transfer>() {
        override fun map(rowView: RowView) = Transfer(rowView.getRow(User::class.java, "a_"), rowView.getRow(User::class.java, "b_"))
    }

    abstract class ListReducer<T> : RowReducer<MutableList<T>, T> {
        abstract fun map(rowView: RowView): T

        override fun container(): MutableList<T> = mutableListOf()

        override fun accumulate(container: MutableList<T>, rowView: RowView) {
            container.add(map(rowView))
        }

        override fun stream(container: MutableList<T>): Stream<T> = container.stream()
    }

    @Test
    fun repeatedMethodAnnotationsRegisterEachPrefixedMapper() {
        val dao = h2Extension.jdbi.onDemand(MethodLevelDao::class.java)

        assertThat(dao.userOrders()).containsExactly(UserOrder(User(1, "alice"), Order(10, "widget")))
    }

    @Test
    fun repeatedMethodAnnotationsResolveByPrefixForTheSameType() {
        val dao = h2Extension.jdbi.onDemand(MethodLevelDao::class.java)

        assertThat(dao.transfers()).containsExactly(Transfer(User(1, "alice"), User(2, "bob")))
    }

    @Test
    fun repeatedTypeAnnotationsRegisterEachPrefixedMapper() {
        val dao = h2Extension.jdbi.onDemand(TypeLevelDao::class.java)

        assertThat(dao.userOrders()).containsExactly(UserOrder(User(1, "alice"), Order(10, "widget")))
    }

    @Test
    fun repeatedAnnotationsCompileIntoTheConfiguredContainer() {
        assertThat(RegisterKotlinMapper::class.java.getAnnotation(java.lang.annotation.Repeatable::class.java).value)
            .isEqualTo(RegisterKotlinMappers::class)
    }
}
