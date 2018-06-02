package org.jdbi.v3.wip;

import java.lang.reflect.Type;
import java.util.Optional;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.argument.Arguments;
import org.jdbi.v3.core.generic.GenericTypes;
import org.jdbi.v3.core.mapper.ColumnMappers;
import org.jdbi.v3.core.spi.JdbiPlugin;

public class OptionalPlugin implements JdbiPlugin {
	@Override
	public void customizeJdbi(Jdbi jdbi) {
		jdbi.registerArgument((type, value, config) -> {
			if (Optional.class.equals(GenericTypes.getErasedType(type))) {
				Optional<Type> t = GenericTypes.findGenericParameter(type, Optional.class);
				if (t.isPresent()) {
					return config.get(Arguments.class).findFor(t.get(), value);
				}
			}
			return Optional.empty();
		});

		jdbi.registerColumnMapper((type, config) -> {
			if (Optional.class.equals(GenericTypes.getErasedType(type))) {
				Optional<Type> t = GenericTypes.findGenericParameter(type, Optional.class);
				if (t.isPresent()) {
					return config.get(ColumnMappers.class).findFor(t.get());
				}
			}
			return Optional.empty();
		});
	}
}
