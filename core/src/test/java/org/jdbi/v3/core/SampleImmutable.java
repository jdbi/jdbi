package org.jdbi.v3.core;

import org.immutables.value.Value;
import org.jdbi.v3.core.mapper.reflect.ImmutableImplementation;

@ImmutableImplementation(ImmutableSampleImmutable.class)
@Value.Immutable
public interface SampleImmutable {
    @Value.Default
    default long id() {
        return 0;
    }

    String name();
    int valueInt();
}
