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
package org.jdbi.v3.core.statement.internal;

import org.jdbi.v3.core.statement.JdbiStatementEvent;

public final class JfrSupport {
    private static boolean present = false;

    private JfrSupport() {
        throw new AssertionError();
    }

    public static void registerEvents() {
        try {
            present = Class.forName("jdk.jfr.FlightRecorder") != null;
            if (present) {
                Holder.registerEvents();
            }
        } catch (ClassNotFoundException | NoClassDefFoundError | UnsatisfiedLinkError ignored) {
            present = false;
        }
    }

    public static OptionalEvent newStatementEvent() {
        if (present) {
            return Holder.newEvent();
        }
        return new NoStatementEvent();
    }

    private static class Holder {
        private Holder() {}

        static void registerEvents() {
            jdk.jfr.FlightRecorder.register(JdbiStatementEvent.class);
        }

        public static OptionalEvent newEvent() {
            return new JdbiStatementEvent();
        }
    }
}
