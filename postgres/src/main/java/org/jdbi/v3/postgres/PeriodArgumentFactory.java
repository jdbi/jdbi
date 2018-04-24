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
package org.jdbi.v3.postgres;

import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.postgresql.util.PGInterval;

import java.sql.Types;
import java.time.Period;

/**
 * Postgres version of argument factory for {@link Period}.
 */
public class PeriodArgumentFactory extends AbstractArgumentFactory<Period> {

    public PeriodArgumentFactory() {
        super(Types.OTHER);
    }

    @Override
    public Argument build(Period period, ConfigRegistry config) {
        PGInterval interval = new PGInterval(period.getYears(), period.getMonths(), period.getDays(), 0, 0, 0);
        return (i, p, cx) -> p.setObject(i, interval, Types.OTHER);
    }
}
