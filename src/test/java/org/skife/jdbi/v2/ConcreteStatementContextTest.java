package org.skife.jdbi.v2;

import junit.framework.TestCase;
import org.junit.Test;

import java.util.Collections;

public class ConcreteStatementContextTest {


    @Test(expected = IllegalArgumentException.class)
    public void testShouldNotBeAbleToCombineGeneratedKeysAndConcurrentUpdatable() throws Exception {
        final ConcreteStatementContext context =
                new ConcreteStatementContext(Collections.<String, Object>emptyMap());

        context.setReturningGeneratedKeys(true);
        context.setConcurrentUpdatable(true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testShouldNotBeAbleToCombineConcurrentUpdatableAndGeneratedKeys() throws Exception {
        final ConcreteStatementContext context =
                new ConcreteStatementContext(Collections.<String, Object>emptyMap());

        context.setConcurrentUpdatable(true);
        context.setReturningGeneratedKeys(true);
    }
}