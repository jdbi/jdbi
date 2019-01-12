package org.jdbi.v3.core.qualifier;

import org.junit.rules.ExternalResource;

// We end up needing junit in compile scope since we borrow JdbiRule, so fool the dependency analyzer.
public class BogusReference {
    @Override
    public String toString() {
        return ExternalResource.class.toString();
    }
}
