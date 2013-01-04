package org.skife.jdbi.v2.sqlobject.subpackage;

import org.skife.jdbi.v2.Something;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SomethingMapper;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

/**
 * User: achauhan
 * Date: 10/18/12
 */
@RegisterMapper(SomethingMapper.class)
public abstract class SomethingYetAgainDao extends SomethingAgainDao {

    @Override
    @SqlQuery("select id, name from something where name <> :name")
    public abstract Something findByNotName(@Bind("name") String name);
}
