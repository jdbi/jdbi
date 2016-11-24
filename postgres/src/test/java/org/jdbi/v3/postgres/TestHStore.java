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
package org.jdbi.v3.postgres;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.util.GenericType;
import org.jdbi.v3.sqlobject.Bind;
import org.jdbi.v3.sqlobject.BindMap;
import org.jdbi.v3.sqlobject.SqlQuery;
import org.jdbi.v3.sqlobject.SqlUpdate;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class TestHStore {

    private static final GenericType<Map<String, String>> STRING_MAP = new GenericType<Map<String, String>>() {
    };

    @ClassRule
    public static PostgresDbRule postgresDbRule = new PostgresDbRule();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private Handle handle;
    private Map<String, String> caps = ImmutableMap.of("yearly", "6000", "monthly", "1500", "daily", "100");

    @BeforeClass
    public static void staticSetUp() {
        postgresDbRule.getSharedHandle().execute("create extension hstore");
    }

    @Before
    public void setUp() throws Exception {
        handle = postgresDbRule.getSharedHandle();
        handle.useTransaction((h, status) -> {
            h.execute("drop table if exists campaigns");
            h.execute("create table campaigns(id int not null, caps hstore)");
            h.insert("insert into campaigns(id, caps) values (1, 'yearly=>10000, monthly=>5000, daily=>200'::hstore)");
            h.insert("insert into campaigns(id, caps) values (2, 'yearly=>1000, monthly=>200, daily=>20'::hstore)");
        });
    }

    @Test
    public void testReadsViaFluentAPI() {
        List<Map<String, String>> caps = handle.createQuery("select caps from campaigns order by id")
                .mapTo(STRING_MAP)
                .list();
        assertThat(caps).isEqualTo(ImmutableList.of(
                ImmutableMap.of("yearly", "10000", "monthly", "5000", "daily", "200"),
                ImmutableMap.of("yearly", "1000", "monthly", "200", "daily", "20")
        ));
    }

    @Test
    public void testHandlesEmptyMap() {
        handle.insert("insert into campaigns(id, caps) values (?,?)", 4, ImmutableMap.of());
        Map<String, String> newCaps = handle.createQuery("select caps from campaigns where id=?")
                .bind(0, 4)
                .mapTo(STRING_MAP)
                .findOnly();
        assertThat(newCaps).isEmpty();
    }

    @Test
    public void testHandlesNulls() {
        handle.insert("insert into campaigns(id, caps) values (?,?)", 4, null);
        Map<String, String> newCaps = handle.createQuery("select caps from campaigns where id=?")
                .bind(0, 4)
                .mapTo(STRING_MAP)
                .findOnly();
        assertThat(newCaps).isNull();
    }

    @Test
    public void testRaisesExceptionWhenReadsWithWrongType() {
        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage("No mapper registered for java.util.Map<java.lang.String, java.lang.Object>");

        handle.createQuery("select caps from campaigns order by id")
                .mapTo(new GenericType<Map<String, Object>>() {})
                .list();
    }

    @Test
    public void testWritesViaFluentApi() {
        handle.insert("insert into campaigns(id, caps) values (?,?)", 3, caps);
        Map<String, String> newCaps = handle.createQuery("select caps from campaigns where id=?")
                .bind(0, 3)
                .mapTo(STRING_MAP)
                .findOnly();
        assertThat(newCaps).isEqualTo(caps);
    }

    @Test
    public void testSqlObjectApi() {
        CampaignDao campaignDao = handle.attach(CampaignDao.class);
        campaignDao.insertCampaign(3, caps);
        assertThat(campaignDao.getCampaignsCaps(3)).isEqualTo(caps);
    }

    @Test
    public void testWritesWithBindMap() {
        CampaignDao campaignDao = handle.attach(CampaignDao.class);
        campaignDao.insertCampaignFromMap(ImmutableMap.of("id", 3, "caps", caps));
        assertThat(campaignDao.getCampaignsCaps(3)).isEqualTo(caps);
    }


    public interface CampaignDao {

        @SqlQuery("select caps from campaigns where id=:id")
        Map<String, String> getCampaignsCaps(@Bind("id") long campaignsId);

        @SqlUpdate("insert into campaigns(id, caps) values (:id, :caps)")
        void insertCampaign(@Bind("id") long campaignsId, Map<String, String> caps);

        @SqlUpdate("insert into campaigns(id, caps) values (:id, :caps)")
        int insertCampaignFromMap(@BindMap Map<String, Object> bindings);
    }
}
