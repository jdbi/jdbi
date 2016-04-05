package org.skife.jdbi.v2.sqlobject.customizers;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Used to constrain maximum rows count. Throws @MaxRowCountExceedException if limit is exceeded.
 * Created by anev on 05/04/16.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface MaxRowCountConstraint {
    /**
     *
     * The max rows constrain.
     */
    int value() default 1;
}
