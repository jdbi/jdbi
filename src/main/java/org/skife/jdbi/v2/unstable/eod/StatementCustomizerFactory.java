package org.skife.jdbi.v2.unstable.eod;

import org.skife.jdbi.v2.tweak.StatementCustomizer;

import java.lang.annotation.Annotation;

public interface StatementCustomizerFactory
{
    public StatementCustomizer createForParameter(Annotation annotation, Object arg);

    StatementCustomizer createForMethod(Annotation annotation);
}
