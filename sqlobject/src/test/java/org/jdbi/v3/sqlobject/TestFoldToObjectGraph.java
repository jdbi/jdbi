/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jdbi.v3.sqlobject;


import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class TestFoldToObjectGraph
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    private Handle handle;
    private Map<String, Team> expected;

    @Before
    public void setUp() throws Exception
    {
        handle = db.getSharedHandle();
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

    @Test
    public void testSqlObjectApi() throws Exception
    {
        Dao dao = handle.attach(Dao.class);
        assertThat(dao.findAllTeams()).isEqualTo(expected);
    }

    public interface Dao
    {
        @SqlQuery("select t.name as teamName, " +
                  "       t.mascot as mascot, " +
                  "       p.name as personName, " +
                  "       p.role as role " +
                  "from team t inner join person p on (t.name = p.team)")
        @RegisterBeanMapper(TeamPersonJoinRow.class)
        Iterator<TeamPersonJoinRow> findAllTeamsAndPeople();

        default Map<String, Team> findAllTeams()
        {
            Iterator<TeamPersonJoinRow> i = findAllTeamsAndPeople();
            Map<String, Team> acc = new HashMap<>();
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
        private final Set<Person> people = new LinkedHashSet<>();

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

            Team that = (Team) o;

            return Objects.equals(this.mascot, that.mascot)
                    && Objects.equals(this.name, that.name)
                    && Objects.equals(this.people, that.people);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(name, mascot, people);
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
