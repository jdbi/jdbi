package org.skife.jdbi.v2.sqlobject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public interface SQLStatementCustomizerFactory
{
    public SQLStatementCustomizer createForMethod(Annotation annotation, Class sqlObjectType, Method method);

    public SQLStatementCustomizer createForType(Annotation annotation, Class sqlObjectType);

    public SQLStatementCustomizer createForParameter(Annotation annotation, Class sqlObjectType, Method method, Object arg);
}
