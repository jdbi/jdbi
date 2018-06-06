package org.jdbi.v3.wip;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.spi.JdbiPlugin;

public class BlockAllDataTypesPlugin implements JdbiPlugin {
	@Override
	public void customizeJdbi(Jdbi jdbi) {
		jdbi.registerArgument((type, value, config) -> {
			throw new UnsupportedOperationException("install an appropriate argument factory for " + type + "!");
		});
		jdbi.registerColumnMapper((type, config) -> {
			throw new UnsupportedOperationException("install an appropriate column mapper for " + type + "!");
		});
	}
}
