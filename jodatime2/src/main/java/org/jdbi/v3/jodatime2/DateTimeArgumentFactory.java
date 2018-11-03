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
package org.jdbi.v3.jodatime2;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.sql.Types;

import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.internal.strategies.LoggableBinderArgument;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.joda.time.DateTime;

/**
 * Bind a {@link DateTime} as a {@link Timestamp}.
 */
public class DateTimeArgumentFactory extends AbstractArgumentFactory<DateTime> {
    public DateTimeArgumentFactory() {
        super(Types.TIMESTAMP);
    }

    @Override
    protected Argument build(DateTime value, ConfigRegistry config) {
        return new LoggableBinderArgument<>(new Timestamp(value.getMillis()), PreparedStatement::setTimestamp);
    }
}
