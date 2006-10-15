package org.skife.jdbi.v2.unstable.eod;

import static org.skife.jdbi.v2.unstable.eod.BindType.Bean;
import org.skife.jdbi.v2.ResultIterator;
import org.skife.jdbi.v2.Something;

import java.util.List;

/**
 *
 */
public interface MyQueries extends DataAccessor
{
    @Select("select id, name from something")
    List<Something> getAllSomethings();

    @Select("select id, name from something where name = ?")
    List<Something> findByName(String name);

    @Select("select id, name from something")
    ResultIterator<Something> ittyAll();

    @Select("select id, name from something where id = :id")
    Something findById(int i);

    @Insert("insert into something (id, name) values (:id, :name)")
    boolean insert(int id, String name);

    @Update("update something set name=? where id=?")
    int updateNameById(String name, int id);

    @Delete("delete from something where id = ?")
    boolean deleteById(int id);

    @Insert("insert into something (id, name) values (:id, :name)")
    @BindBy(Bean)
    boolean insert(Something s);
}
