package org.skife.jdbi.v2.sqlobject;


import org.skife.jdbi.v2.sqlobject.stringtemplate.UseStringTemplate3StatementLocator;

@UseStringTemplate3StatementLocator("/org/skife/jdbi/v2/sqlobject/TestKombucha.sql.stg")
public interface Testkombucha  {

    @SqlQuery
    public String getIngredients();
}
