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
package org.jdbi.core.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import com.google.common.collect.ImmutableList;
import org.jdbi.core.Handle;
import org.jdbi.core.junit5.H2DatabaseExtension;
import org.jdbi.core.mapper.reflect.BeanMapper;
import org.jdbi.core.statement.StatementContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestIssue2016 {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance().withInitializer(H2DatabaseExtension.SOMETHING_INITIALIZER);

    private Handle h;

    @BeforeEach
    void setUp() {
        h = h2Extension.getSharedHandle();
    }

    @Test
    public void testIssue2016() {
        h.registerRowMapper(Tag.class, new TagMapper());
        h.registerRowMapper(ModBusTag.class, BeanMapper.of(ModBusTag.class));
        h.registerRowMapper(SNMPTag.class, BeanMapper.of(SNMPTag.class));
        h.registerRowMapper(EIPTag.class, BeanMapper.of(EIPTag.class));
        h.registerRowMapper(S7Tag.class, BeanMapper.of(S7Tag.class));
        h.registerRowMapper(ICMPTag.class, BeanMapper.of(ICMPTag.class));

        h.execute("create table protocols (id integer, protocol VARCHAR(32), state VARCHAR(32))");
        h.execute("insert into protocols (id, protocol, state) values (1, 'ModBus', 'on')");
        h.execute("insert into protocols (id, protocol, state) values (2, 'ModBus', 'off')");
        h.execute("insert into protocols (id, protocol, state) values (3, 'SNMP', 'on')");
        h.execute("insert into protocols (id, protocol, state) values (4, 'SNMP', 'off')");
        h.execute("insert into protocols (id, protocol, state) values (5, 'EIP', 'on')");
        h.execute("insert into protocols (id, protocol, state) values (6, 'EIP', 'off')");
        h.execute("insert into protocols (id, protocol, state) values (7, 'S7', 'on')");
        h.execute("insert into protocols (id, protocol, state) values (8, 'S7', 'off')");
        h.execute("insert into protocols (id, protocol, state) values (9, 'ICMP', 'on')");
        h.execute("insert into protocols (id, protocol, state) values (10, 'ICMP', 'off')");

        ModBusTag bean1 = new ModBusTag();
        bean1.setId(1);
        bean1.setState("on");

        ModBusTag bean2 = new ModBusTag();
        bean2.setId(2);
        bean2.setState("off");

        SNMPTag bean3 = new SNMPTag();
        bean3.setState("on");

        SNMPTag bean4 = new SNMPTag();
        bean4.setState("off");

        EIPTag bean5 = new EIPTag();
        bean5.setState("on");

        EIPTag bean6 = new EIPTag();
        bean6.setState("off");

        S7Tag bean7 = new S7Tag();
        bean7.setId(7);
        bean7.setState("on");

        S7Tag bean8 = new S7Tag();
        bean8.setId(8);
        bean8.setState("off");

        ICMPTag bean9 = new ICMPTag();
        bean9.setId(9);
        bean9.setState("on");

        ICMPTag bean10 = new ICMPTag();
        bean10.setId(10);
        bean10.setState("off");

        List<? extends Tag> expectedTags = ImmutableList.of(bean1, bean2, bean3, bean4, bean5, bean6, bean7, bean8, bean9, bean10);

        // use registered mapper
        List<Tag> tags = h.createQuery("SELECT * FROM protocols order by id")
            .mapTo(Tag.class)
            .list();

        assertThat(tags).containsExactlyElementsOf(expectedTags);

        // use explicit mapper instance.
        tags = h.createQuery("SELECT * FROM protocols order by id")
            .map(new TagMapper())
            .list();

        assertThat(tags).containsExactlyElementsOf(expectedTags);

        // test wildcards

        List<? extends Tag> wildcardTags = h.createQuery("SELECT * FROM protocols order by id")
            .mapTo(Tag.class)
            .list();

        assertThat((List<Tag>) wildcardTags).containsExactlyElementsOf(expectedTags);

        // use explicit mapper instance.
        wildcardTags = h.createQuery("SELECT * FROM protocols order by id")
            .map(new TagMapper())
            .list();

        assertThat((List<Tag>) wildcardTags).containsExactlyElementsOf(expectedTags);
    }

    public abstract static class Tag {

        public abstract String getState();
    }

    public static class ModBusTag extends Tag {

        private int id;
        private String state;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        @Override
        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ModBusTag modBusTag = (ModBusTag) o;
            return id == modBusTag.id && Objects.equals(state, modBusTag.state);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, state);
        }
    }

    public static class SNMPTag extends Tag {

        private String state;

        @Override
        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            SNMPTag snmpTag = (SNMPTag) o;
            return Objects.equals(state, snmpTag.state);
        }

        @Override
        public int hashCode() {
            return Objects.hash(state);
        }
    }

    public abstract static class AbstractTag extends Tag {

        protected String state;

        @Override
        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }
    }

    public abstract static class AbstractIdTag extends AbstractTag {

        protected int id;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }
    }

    public static class EIPTag extends AbstractTag {

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            EIPTag tag = (EIPTag) o;
            return Objects.equals(state, tag.state);
        }

        @Override
        public int hashCode() {
            return Objects.hash(state);
        }

    }

    public static class S7Tag extends AbstractIdTag {

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            S7Tag tag = (S7Tag) o;
            return id == tag.id && Objects.equals(state, tag.state);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, state);
        }

    }

    public static class ICMPTag extends AbstractIdTag {

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ICMPTag tag = (ICMPTag) o;
            return id == tag.id && Objects.equals(state, tag.state);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, state);
        }

    }

    public static class TagMapper implements RowMapper<Tag> {

        @Override
        public Tag map(ResultSet rs, StatementContext ctx) throws SQLException {
            String proto = rs.getString("protocol");
            switch (proto) {
                case "ModBus":
                    return mapTo(rs, ctx, ModBusTag.class);
                case "SNMP":
                    return mapTo(rs, ctx, SNMPTag.class);
                case "EIP":
                    return mapTo(rs, ctx, EIPTag.class);
                case "S7":
                    return mapTo(rs, ctx, S7Tag.class);
                case "ICMP":
                    return mapTo(rs, ctx, ICMPTag.class);
                default:
                    throw new NoSuchMapperException("Can not map " + proto);
            }
        }

        private static <T extends Tag> T mapTo(ResultSet rs, StatementContext ctx, Class<T> targetClass) throws SQLException {
            return ctx.getConfig().get(Mappers.class)
                .findFor(targetClass)
                .orElseThrow(() ->
                    new NoSuchMapperException(String.format("No mapper registered for %s class", targetClass))
                )
                .map(rs, ctx);
        }
    }
}
