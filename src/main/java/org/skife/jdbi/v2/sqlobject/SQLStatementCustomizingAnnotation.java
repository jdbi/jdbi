package org.skife.jdbi.v2.sqlobject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface SQLStatementCustomizingAnnotation
{
    Class<? extends SQLStatementCustomizerFactory> value();
}
