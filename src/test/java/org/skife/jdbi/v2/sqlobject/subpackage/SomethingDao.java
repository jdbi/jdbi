package org.skife.jdbi.v2.sqlobject.subpackage;

import org.skife.jdbi.v2.Something;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SomethingMapper;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

@RegisterMapper(SomethingMapper.class)
public abstract class SomethingDao
{
    @SqlUpdate("insert into something (id, name) values (:id, :name)")
    public abstract void insert(@Bind("id") int id, @Bind("name") String name);

    @SqlQuery("select id, name from something where id = :id")
    public abstract Something findById(@Bind("id") int id);

    public Something findByIdHeeHee(int id) {
        return findById(id);
    }
    
    public abstract Something findByName(String name);

    public abstract Something findByNotName(String name);

    public abstract void totallyBroken();

}
