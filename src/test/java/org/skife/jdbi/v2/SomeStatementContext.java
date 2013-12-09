package org.skife.jdbi.v2;

import java.util.Map;

public class SomeStatementContext extends ConcreteStatementContext {

    public SomeStatementContext(final Map<String, Object> globalAttributes) {
        super(globalAttributes);
    }
}
