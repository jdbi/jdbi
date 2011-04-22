package org.skife.jdbi.v2.sqlobject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public interface SQLStatementCustomizerFactory
{
    public SQLStatementCustomizer create(Annotation annotation, Class sqlObjectType, Method method);
}
