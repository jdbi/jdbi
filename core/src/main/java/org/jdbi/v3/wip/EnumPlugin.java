package org.jdbi.v3.wip;

import java.sql.Types;
import java.util.Optional;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.spi.JdbiPlugin;

public class EnumPlugin implements JdbiPlugin {
	@Override
	public void customizeJdbi(Jdbi jdbi) {
		jdbi.registerArgument((type, value, config) -> {
            if (type instanceof Class && Enum.class.isAssignableFrom((Class) type)) {
                if (value == null) {
                    return Optional.of((position, statement, ctx) -> statement.setNull(position, Types.VARCHAR));
                }
            }
            return Optional.empty();
        });

		jdbi.registerColumnMapper((type, config) -> {
            if (type instanceof Class && Enum.class.isAssignableFrom((Class) type)) {
                return Optional.of((r, columnNumber, ctx) -> r.getObject(columnNumber) == null
                    ? null
                    : Enum.valueOf((Class) type, r.getString(columnNumber)));
            } else {
                return Optional.empty();
            }
        });
	}
}
