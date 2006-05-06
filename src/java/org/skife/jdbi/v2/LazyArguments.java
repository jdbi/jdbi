package org.skife.jdbi.v2;

import org.skife.jdbi.v2.tweak.Argument;

/**
 * 
 */
interface LazyArguments
{
    Argument find(String name);
}
