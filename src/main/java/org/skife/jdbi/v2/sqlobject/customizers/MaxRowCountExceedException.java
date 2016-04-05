package org.skife.jdbi.v2.sqlobject.customizers;

/**
 * Created by anev on 05/04/16.
 */
public class MaxRowCountExceedException extends RuntimeException {
    public MaxRowCountExceedException(int max) {
        super("Maximum rows count exceeded, threshold is " + max);
    }
}
