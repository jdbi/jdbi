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
package org.jdbi.v3.examples;

import java.util.List;
import java.util.stream.Collector;

import com.google.common.base.Functions;
import com.google.common.collect.Multimap;
import org.jdbi.v3.examples.order.Order;

import static com.google.common.collect.ImmutableListMultimap.toImmutableListMultimap;
import static org.jdbi.v3.examples.order.OrderSupport.withOrders;

/**
 * Map a list or results based on an attribute onto a map. Multiple results can map on the same attribute, so using a Guava Multimap (which is a map of lists)
 * solves that problem.
 * <p>
 * Also shows the use of {@link org.jdbi.v3.core.result.ResultIterable#collect(Collector)}.
 */
@SuppressWarnings({"PMD.SystemPrintln"})
public final class ResultsAsMultimap {

    private ResultsAsMultimap() {
        throw new AssertionError("ResultsAsMultimap can not be instantiated");
    }

    public static void main(String... args) throws Exception {
        withOrders(jdbi -> {

            // find five user ids in the database.
            List<Integer> userIds = jdbi.withHandle(
                handle -> handle.createQuery("SELECT DISTINCT user_id FROM orders LIMIT 5")
                    .mapTo(Integer.class)
                    .list());

            // pull out all orders that match the user ids
            Multimap<Integer, Order> results = jdbi.withHandle(
                handle -> handle.createQuery("SELECT * from orders WHERE user_id IN (<user_id>)")
                    .bindList("user_id", userIds)
                    .mapTo(Order.class)
                    .collect(toImmutableListMultimap(Order::getUserId, Functions.identity())));

            // display results
            results.asMap().forEach((k, v) -> System.out.printf("%d -> %s%n", k, v));
        });
    }
}
