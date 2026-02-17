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
package org.jdbi.examples.order;

import java.util.Objects;
import java.util.StringJoiner;

public class Order {
    private final int id;
    private final int userId;
    private final String comment;
    private final String address;

    public Order(int id, int userId, String comment, String address) {
        this.id = id;
        this.userId = userId;
        this.comment = comment;
        this.address = address;
    }

    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public String getComment() {
        return comment;
    }

    public String getAddress() {
        return address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Order order = (Order) o;
        return id == order.id && userId == order.userId && Objects.equals(comment, order.comment) && Objects.equals(address, order.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, comment, address);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Order.class.getSimpleName() + "[", "]")
            .add("id=" + id)
            .add("userId=" + userId)
            .add("comment='" + comment + "'")
            .add("address='" + address + "'")
            .toString();
    }
}
