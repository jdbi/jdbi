package org.jdbi.v3.stringtemplate4;

import java.util.List;

import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.customizer.BindMethodsList;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.customizer.DefineNamedBindings;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestBindMethodsListWithStringTemplate {
    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2()
        .withPlugin(new SqlObjectPlugin());

    public record Boo(String a, String b) {}

    public interface Dao {
        @UseStringTemplateEngine
        @DefineNamedBindings
        @SqlQuery("SELECT <if(flag)>('c', 'd') IN (<list>)<else>TRUE<endif>")
        boolean test2(@Define boolean flag,
            @BindMethodsList(methodNames = { "a", "b" }) List<Boo> list);
    }

    @Test
    public void reproduceIssue() {
        Dao dao = h2Extension.getJdbi().onDemand(Dao.class);

        List<Boo> list = List.of(
            new Boo("c", "d"),
            new Boo("e", "f")
        );

        assertTrue(dao.test2(true, list));
    }
}
