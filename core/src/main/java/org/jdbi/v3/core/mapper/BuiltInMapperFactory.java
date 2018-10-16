package org.jdbi.v3.core.mapper;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jdbi.v3.core.array.SqlArrayMapperFactory;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.internal.JdbiStreams;

@Deprecated
// TODO remove this entire class, move functionality to BuiltInSupportPlugin
public class BuiltInMapperFactory implements ColumnMapperFactory {
    private static final List<ColumnMapperFactory> FACTORIES = new ArrayList<>();

    static {
        FACTORIES.add(new EnumMapperFactory());
        FACTORIES.add(new OptionalMapperFactory());
        FACTORIES.add(new PrimitiveMapperFactory());
        FACTORIES.add(new BoxedMapperFactory());
        FACTORIES.add(new EssentialsMapperFactory());
        FACTORIES.add(new InternetMapperFactory());
        FACTORIES.add(new SqlTimeMapperFactory());
        FACTORIES.add(new JavaTimeMapperFactory());
        FACTORIES.add(new OptionalMapperFactory());
        FACTORIES.add(new SqlArrayMapperFactory());
    }

    public Optional<ColumnMapper<?>> build(Type type, ConfigRegistry config) {
        return FACTORIES.stream()
            .flatMap(factory -> JdbiStreams.toStream(factory.build(type, config)))
            .findFirst();
    }
}
