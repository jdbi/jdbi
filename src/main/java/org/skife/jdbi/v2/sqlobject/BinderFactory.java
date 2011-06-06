package org.skife.jdbi.v2.sqlobject;

import java.lang.annotation.Annotation;

interface BinderFactory
{
    Binder build(Annotation annotation);
}
