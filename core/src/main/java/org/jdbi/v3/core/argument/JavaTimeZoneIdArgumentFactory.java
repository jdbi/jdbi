package org.jdbi.v3.core.argument;

import org.jdbi.v3.core.config.ConfigRegistry;

import java.sql.Types;
import java.time.ZoneId;

public class JavaTimeZoneIdArgumentFactory extends AbstractArgumentFactory<ZoneId> {
    public JavaTimeZoneIdArgumentFactory() {
        super(Types.VARCHAR);
    }

    @Override
    protected Argument build(ZoneId value, ConfigRegistry config) {
        return (i, p, ctx) -> p.setString(i, value.getId());
    }
}
