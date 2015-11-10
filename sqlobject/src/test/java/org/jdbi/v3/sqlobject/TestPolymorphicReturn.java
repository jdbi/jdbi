package org.jdbi.v3.sqlobject;

import static org.assertj.core.api.Assertions.assertThat;

import org.jdbi.v3.H2DatabaseRule;
import org.jdbi.v3.sqlobject.customizers.MapTo;
import org.jdbi.v3.sqlobject.customizers.RegisterMapperFactory;
import org.jdbi.v3.tweak.BeanMapperFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestPolymorphicReturn {
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();

    private SheepDao dao;

    @Before
    public void makeSheep() {
        db.getSharedHandle().execute("insert into something(name, intValue) values('Fluffy', 5)");
        dao = db.getSharedHandle().attach(SheepDao.class);
    }

    @Test
    public void testPolymorphicReturnSuperclass() throws Exception {
        Sheep normalSheep = dao.get(Sheep.class, "Fluffy");
        assertThat(normalSheep.getName()).isEqualTo("Fluffy");
    }

    @Test
    public void testPolymorphicReturnSubclass() throws Exception {
        FlyingSheep flyingSheep = dao.get(FlyingSheep.class, "Fluffy");
        assertThat(flyingSheep.getName()).isEqualTo("Fluffy");
        assertThat(flyingSheep.getNumWings()).isEqualTo(5);
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testBadArg() throws Exception {
        dao.getBad("Fluffy is sad :(");
    }


    @RegisterMapperFactory(BeanMapperFactory.class)
    interface SheepDao {
        @SqlQuery("select name, intValue as numWings from something where name=:name")
        <T extends Sheep> T get(@MapTo Class<T> klass, String name);

        @SqlQuery("baaaaaa")
        Sheep getBad(@MapTo String baaaaa);
    }

    public static class Sheep {
        private String name;

        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
    }

    public static class FlyingSheep extends Sheep {
        private int numWings;

        public int getNumWings() {
            return numWings;
        }
        public void setNumWings(int numWings) {
            this.numWings = numWings;
        }
    }
}
