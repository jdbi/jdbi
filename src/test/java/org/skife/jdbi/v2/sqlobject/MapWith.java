package org.skife.jdbi.v2.sqlobject;

import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface MapWith
{
    Class<? extends ResultSetMapper> value();
}
