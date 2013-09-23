package org.skife.jdbi.v2.docs;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.FoldController;
import org.skife.jdbi.v2.Folder3;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.helpers.MapResultAsBean;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class TestFoldToObjectGraph
{
    private DBI dbi;
    private Handle handle;
    private Map<String, Team> expected;

    @Before
    public void setUp() throws Exception
    {
        dbi = new DBI("jdbc:h2:mem:" + UUID.randomUUID());
        handle = dbi.open();
        handle.execute("create table team ( name varchar(100), " +
                       "                    mascot varchar(100)," +
                       "                    primary key (name) )");

        handle.execute("create table person( name varchar(100), " +
                       "                     role varchar(100), " +
                       "                     team varchar(100)," +
                       "                     primary key (name)," +
                       "                     foreign key (team) references team(name) )");

        handle.prepareBatch("insert into team (name, mascot) values (?, ?)")
              .add("A-Team", "The Van")
              .add("Hogan's Heroes", "The Tunnel")
              .execute();

        handle.prepareBatch("insert into person (name, role, team) values (?, ?, ?)")
              .add("Kinchloe", "comms", "Hogan's Heroes")
              .add("Carter", "bombs", "Hogan's Heroes")
              .add("Murdoch", "driver", "A-Team")
              .add("Peck", "face", "A-Team")
              .execute();

        Team ateam = new Team("A-Team", "The Van");
        ateam.getPeople().add(new Person("Murdoch", "driver"));
        ateam.getPeople().add(new Person("Peck", "face"));

        Team hogans = new Team("Hogan's Heroes", "The Tunnel");
        hogans.getPeople().add(new Person("Kinchloe", "comms"));
        hogans.getPeople().add(new Person("Carter", "bombs"));

        this.expected = ImmutableMap.of("Hogan's Heroes", hogans,
                                        "A-Team", ateam);


    }

    @After
    public void tearDown() throws Exception
    {
        handle.close();
    }

    @Test
    public void testFluentApi() throws Exception
    {
        Map<String, Team> teams = handle.createQuery("select t.name as teamName, " +
                                                     "       t.mascot as mascot, " +
                                                     "       p.name as personName, " +
                                                     "       p.role as role " +
                                                     "from team t inner join person p on (t.name = p.team)")
                                        .map(TeamPersonJoinRow.class)
                                        .fold(Maps.<String, Team>newHashMap(), new TeamFolder());

        assertThat(teams, equalTo(expected));

    }

    public static class TeamFolder implements Folder3<Map<String, Team>, TeamPersonJoinRow>
    {
        @Override
        public Map<String, Team> fold(Map<String, Team> acc,
                                      TeamPersonJoinRow row,
                                      FoldController control,
                                      StatementContext ctx) throws SQLException
        {
            if (!acc.containsKey(row.getTeamName())) {
                acc.put(row.getTeamName(), new Team(row.getTeamName(), row.getMascot()));
            }

            acc.get(row.getTeamName()).getPeople().add(new Person(row.getPersonName(), row.getRole()));
            return acc;
        }
    }

    @Test
    public void testSqlObjectApi() throws Exception
    {
        Dao dao = handle.attach(Dao.class);
        assertThat(dao.findAllTeams(), equalTo(expected));

    }

    public static abstract class Dao
    {
        @SqlQuery("select t.name as teamName, " +
                  "       t.mascot as mascot, " +
                  "       p.name as personName, " +
                  "       p.role as role " +
                  "from team t inner join person p on (t.name = p.team)")
        @MapResultAsBean
        public abstract Iterator<TeamPersonJoinRow> findAllTeamsAndPeople();

        public Map<String, Team> findAllTeams()
        {
            Iterator<TeamPersonJoinRow> i = findAllTeamsAndPeople();
            Map<String, Team> acc = Maps.newHashMap();
            while (i.hasNext()) {
                TeamPersonJoinRow row = i.next();
                if (!acc.containsKey(row.getTeamName())) {
                    acc.put(row.getTeamName(), new Team(row.getTeamName(), row.getMascot()));
                }

                acc.get(row.getTeamName()).getPeople().add(new Person(row.getPersonName(), row.getRole()));
            }
            return acc;
        }
    }


    public static class Team
    {
        private final String name;
        private final String mascot;
        private final Set<Person> people = new LinkedHashSet<Person>();

        public Team(String name, String mascot)
        {
            this.name = name;
            this.mascot = mascot;
        }

        public String getName()
        {
            return name;
        }

        public String getMascot()
        {
            return mascot;
        }

        public Set<Person> getPeople()
        {
            return this.people;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Team team = (Team) o;

            if (mascot != null ? !mascot.equals(team.mascot) : team.mascot != null) return false;
            return !(name != null ? !name.equals(team.name) : team.name != null) &&
                   !(people != null ? !people.equals(team.people) : team.people != null);

        }

        @Override
        public int hashCode()
        {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + (mascot != null ? mascot.hashCode() : 0);
            result = 31 * result + (people != null ? people.hashCode() : 0);
            return result;
        }
    }

    public static class Person
    {
        private final String name;
        private final String role;

        public Person(String name, String role)
        {
            this.name = name;
            this.role = role;
        }

        public String getName()
        {
            return name;
        }

        public String getRole()
        {
            return role;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Person person = (Person) o;

            return name.equals(person.name) && role.equals(person.role);

        }

        @Override
        public int hashCode()
        {
            int result = name.hashCode();
            result = 31 * result + role.hashCode();
            return result;
        }
    }

    public static class TeamPersonJoinRow
    {
        private String teamName;
        private String mascot;
        private String personName;
        private String role;

        public String getTeamName()
        {
            return teamName;
        }

        public String getMascot()
        {
            return mascot;
        }

        public String getPersonName()
        {
            return personName;
        }

        public String getRole()
        {
            return role;
        }

        public void setTeamName(String teamName)
        {
            this.teamName = teamName;
        }

        public void setMascot(String mascot)
        {
            this.mascot = mascot;
        }

        public void setPersonName(String personName)
        {
            this.personName = personName;
        }

        public void setRole(String role)
        {
            this.role = role;
        }
    }
}
