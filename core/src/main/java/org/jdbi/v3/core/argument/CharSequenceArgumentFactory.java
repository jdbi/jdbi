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
package org.jdbi.v3.core.argument;

import java.sql.Types;
import java.util.Objects;

import org.jdbi.v3.core.config.ConfigRegistry;

/**
 * An {@link ArgumentFactory} for arguments that implement {@link CharSequence}.<p>
 *
 * The factory is registered by default in {@link Arguments} before other more specific or
 * user-defined factories such {@link EssentialsArgumentFactory} (which has
 * explicit support for {@link String} arguments).<br>
 * The factory converts arguments to String by calling their {@code toString()} method
 * and treats them as sql type {@link Types#VARCHAR}.
 *
 * @since 3.30.1
 */
public class CharSequenceArgumentFactory extends AbstractArgumentFactory<CharSequence> {

    public CharSequenceArgumentFactory() {
        super(Types.VARCHAR);
    }

    @Override
    protected Argument build(CharSequence value, ConfigRegistry config) {
        return (position, statement, ctx) -> statement.setString(position, Objects.toString(value, null));
    }

}
