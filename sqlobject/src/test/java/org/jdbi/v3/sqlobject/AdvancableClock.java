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
package org.jdbi.v3.sqlobject;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalUnit;

public class AdvancableClock extends Clock {
    private ZonedDateTime now;

    public AdvancableClock(ZonedDateTime now) {
        this.now = now;
    }

    @Override
    public ZoneId getZone() {
        return now.getZone();
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new AdvancableClock(now.withZoneSameInstant(zone));
    }

    @Override
    public Instant instant() {
        return now.toInstant();
    }

    public void advance(long amountToAdd, TemporalUnit unit) {
        now = now.plus(amountToAdd, unit);
    }
}
