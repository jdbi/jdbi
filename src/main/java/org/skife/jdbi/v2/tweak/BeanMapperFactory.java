package org.skife.jdbi.v2.tweak;

import org.skife.jdbi.v2.BeanMapper;
import org.skife.jdbi.v2.ResultSetMapperFactory;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.util.NamingStrategy;

public class BeanMapperFactory implements ResultSetMapperFactory
{
   private final NamingStrategy dbFormattingStrategy;
   private final NamingStrategy propertyFormattingStrategy;

   public BeanMapperFactory(NamingStrategy dbFormattingStrategy, NamingStrategy propertyFormattingStrategy) {
      this.dbFormattingStrategy = dbFormattingStrategy;
      this.propertyFormattingStrategy = propertyFormattingStrategy;
   }

   @Override
    public boolean accepts(Class type, StatementContext ctx)
    {
        return true;
    }

    @Override
    public ResultSetMapper mapperFor(Class type, StatementContext ctx)
    {
        return new BeanMapper(type, dbFormattingStrategy, propertyFormattingStrategy);
    }
}
