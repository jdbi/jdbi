package org.skife.jdbi.v2.tweak;

/**
 * 
 */
public interface StatementLocator
{
    public String locate(String name) throws Exception;
}
