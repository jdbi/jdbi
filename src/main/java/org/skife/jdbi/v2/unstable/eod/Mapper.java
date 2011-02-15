package org.skife.jdbi.v2.unstable.eod;

import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Mapper
{
    Class<? extends ResultSetMapper> value();
}
