package org.skife.jdbi.v2.sqlobject;

import java.lang.annotation.Annotation;

/**
 * Factor for building {@link Binder} instances. This interface is used by
 * the {@link BindingAnnotation}
 */
public interface BinderFactory
{
    /**
     * Called to build a Binder
     * @param annotation the {@link BindingAnnotation} which lead to this call
     * @return a binder to use
     */
    Binder build(Annotation annotation);
}
