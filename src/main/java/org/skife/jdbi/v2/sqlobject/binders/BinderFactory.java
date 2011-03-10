package org.skife.jdbi.v2.sqlobject.binders;

import java.lang.annotation.Annotation;

public interface BinderFactory
{
    Binder build(Annotation annotation);
}
