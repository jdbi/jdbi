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

import java.lang.reflect.Method;

import org.jdbi.v3.core.statement.JdbiStatementEvent;

public final class JfrSupport {
    private static final boolean JFR_AVAILABLE = ModuleLayer.boot().findModule("jdk.jfr").isPresent();
    private static final boolean FLIGHT_RECORDER_AVAILABLE;

    private JfrSupport() {
        throw new AssertionError("JfrSupport can not be instantiated");
    }

    static {
        boolean flightRecorderAvailable = false;

        if (isJfrAvailable()) {
            try {
                Class<?> flightRecorder = Class.forName("jdk.jfr.FlightRecorder");
                Method register = flightRecorder.getMethod("register", Class.class);
                register.invoke(null, JdbiStatementEvent.class);

                Method isAvailable = flightRecorder.getMethod("isAvailable");
                flightRecorderAvailable = (boolean) isAvailable.invoke(null);

            } catch (ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        FLIGHT_RECORDER_AVAILABLE = flightRecorderAvailable;
    }

    public static boolean isJfrAvailable() {
        return JFR_AVAILABLE;
    }

    public static boolean isFlightRecorderAvailable() {
        return FLIGHT_RECORDER_AVAILABLE;
    }

    public static OptionalEvent newStatementEvent() {
        if (isJfrAvailable()) {
            return Holder.newEvent();
        } else {
            return new NoStatementEvent();
        }
    }

    private static final class Holder {
        private Holder() {}

        public static OptionalEvent newEvent() {
            return new JdbiStatementEvent();
        }
    }
}
