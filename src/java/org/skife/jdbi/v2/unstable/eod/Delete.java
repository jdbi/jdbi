package org.skife.jdbi.v2.unstable.eod;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 *
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Delete
{
    String value();
}
