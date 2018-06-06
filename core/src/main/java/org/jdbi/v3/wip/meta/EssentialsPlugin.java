package org.jdbi.v3.wip.meta;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.spi.JdbiPlugin;
import org.jdbi.v3.wip.BlockAllDataTypesPlugin;
import org.jdbi.v3.wip.EnumPlugin;
import org.jdbi.v3.wip.OptionalPlugin;
import org.jdbi.v3.wip.categories.JdbcBasicsPlugin;
import org.jdbi.v3.wip.categories.LegacyTimePlugin;
import org.jdbi.v3.wip.categories.OptionalPrimitivesPlugin;
import org.jdbi.v3.wip.categories.PrimitivesPlugin;
import org.jdbi.v3.wip.categories.UnreliableJavaTimePlugin;

public class EssentialsPlugin implements JdbiPlugin {
	public static final JdbiPlugin CORE = new EssentialsPlugin(false);
	public static final JdbiPlugin CONTROVERSIAL = new EssentialsPlugin(true);

	private final boolean controversial;

	private EssentialsPlugin(boolean controversial) {
		this.controversial = controversial;
	}

	@Override
	public void customizeJdbi(Jdbi jdbi) {
		jdbi.installPlugin(new BlockAllDataTypesPlugin());
		jdbi.installPlugin(new EnumPlugin());
		jdbi.installPlugin(new OptionalPlugin());
		jdbi.installPlugin(new PrimitivesPlugin());
		jdbi.installPlugin(new OptionalPrimitivesPlugin());
		jdbi.installPlugin(new JdbcBasicsPlugin());

		if (controversial) {
			jdbi.installPlugin(new LegacyTimePlugin());
			jdbi.installPlugin(new UnreliableJavaTimePlugin());
		}
	}
}
