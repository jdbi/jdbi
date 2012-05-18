package org.skife.jdbi.v2.sqlobject;

import org.skife.jdbi.v2.TransactionIsolationLevel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Transaction
{
    TransactionIsolationLevel value() default TransactionIsolationLevel.INVALID_LEVEL;
}
