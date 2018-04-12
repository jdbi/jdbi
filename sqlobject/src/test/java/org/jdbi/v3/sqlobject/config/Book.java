package org.jdbi.v3.sqlobject.config;

import org.immutables.value.Value;
import org.jdbi.v3.core.mapper.reflect.ImmutableImplementation;

@ImmutableImplementation(ImmutableBook.class)
@Value.Immutable
public interface Book {
    int id();
    String name();
}
