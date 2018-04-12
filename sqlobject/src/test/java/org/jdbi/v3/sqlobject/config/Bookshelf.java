package org.jdbi.v3.sqlobject.config;

import org.immutables.value.Value;
import org.jdbi.v3.core.mapper.reflect.ImmutableImplementation;

import java.util.List;

@ImmutableImplementation(ImmutableBookshelf.class)
@Value.Immutable
public interface Bookshelf {
    int id();
    String name();
    List<Book> books();
}
