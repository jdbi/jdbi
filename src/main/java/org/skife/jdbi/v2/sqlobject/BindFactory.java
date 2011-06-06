package org.skife.jdbi.v2.sqlobject;

import java.lang.annotation.Annotation;

class BindFactory implements BinderFactory
{
    public Binder build(Annotation annotation)
    {
        Bind bind = (Bind) annotation;
        try {
            return bind.binder().newInstance();
        }
        catch (Exception e) {
            throw new IllegalStateException("unable to instantiate specified binder", e);
        }
    }
}
