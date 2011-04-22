package org.skife.jdbi.v2.sqlobject;

import org.skife.jdbi.v2.tweak.StatementLocator;

import java.lang.annotation.Annotation;

public interface StatementLocatorFactory
{
    public StatementLocator create(Annotation anno, Class sqlObjectType, StatementLocator parent);
}
