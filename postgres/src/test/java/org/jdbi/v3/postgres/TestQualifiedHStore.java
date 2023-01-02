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

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.mapper.NoSuchMapperException;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.sqlobject.SingleValue;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindMap;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestQualifiedHStore {

    private static final GenericType<Map<String, String>> STRING_MAP = new GenericType<Map<String, String>>() {};

    @RegisterExtension
    public static EmbeddedPgExtension pg = MultiDatabaseBuilder.instanceWithDefaults()
        .withDatabasePreparer(ds -> Jdbi.create(ds).withHandle(h -> h.execute("create extension hstore")))
        .build();

    @RegisterExtension
    public JdbiExtension pgExtension = JdbiExtension.postgres(pg).withPlugins(new SqlObjectPlugin(), new PostgresPlugin())
        .withInitializer((ds, h) -> h.useTransaction(th -> {
            th.execute("drop table if exists campaigns");
            th.execute("create table campaigns(id int not null, caps hstore)");
            th.execute("insert into campaigns(id, caps) values (1, 'yearly=>10000, monthly=>5000, daily=>200'::hstore)");
            th.execute("insert into campaigns(id, caps) values (2, 'yearly=>1000, monthly=>200, daily=>20'::hstore)");
        }));

    private Handle handle;
    private final Map<String, String> caps = ImmutableMap.of("yearly", "6000", "monthly", "1500", "daily", "100");

    @BeforeEach
    public void setUp() {
        handle = pgExtension.openHandle();
    }

    @AfterEach
    public void tearDown() {
        handle.close();
    }

    @Test
    public void testReadsViaFluentAPI() {
        List<Map<String, String>> initialCaps = handle.createQuery("select caps from campaigns order by id")
            .mapTo(QualifiedType.of(STRING_MAP).with(HStore.class))
            .list();
        assertThat(initialCaps).isEqualTo(ImmutableList.of(
            ImmutableMap.of("yearly", "10000", "monthly", "5000", "daily", "200"),
            ImmutableMap.of("yearly", "1000", "monthly", "200", "daily", "20")));
    }

    @Test
    public void testHandlesEmptyMap() {
        handle.execute("insert into campaigns(id, caps) values (?,?)", 4, ImmutableMap.of());
        Map<String, String> newCaps = handle.createQuery("select caps from campaigns where id=?")
                .bind(0, 4)
                .mapTo(QualifiedType.of(STRING_MAP).with(HStore.class))
                .one();
        assertThat(newCaps).isEmpty();
    }

    @Test
    public void testHandlesNulls() {
        handle.execute("insert into campaigns(id, caps) values (?,?)", 4, null);
        Map<String, String> newCaps = handle.createQuery("select caps from campaigns where id=?")
                .bind(0, 4)
                .mapTo(QualifiedType.of(STRING_MAP).with(HStore.class))
                .one();
        assertThat(newCaps).isNull();
    }

    @Test
    public void testRaisesExceptionWhenReadsWithWrongType() {
        assertThatThrownBy(() -> handle.createQuery("select caps from campaigns order by id")
                .mapTo(QualifiedType.of(new GenericType<Map<String, Object>>() {}).with(HStore.class))
                .list())
            .isInstanceOf(NoSuchMapperException.class)
            .hasMessageContaining("No mapper registered for type @org.jdbi.v3.postgres.HStore() java.util.Map<java.lang.String, java.lang.Object>");
    }

    @Test
    public void testWritesViaFluentApi() {
        handle.createUpdate("insert into campaigns(id, caps) values (:id, :caps)")
            .bind("id", 3)
            .bindByType("caps", caps, QualifiedType.of(STRING_MAP).with(HStore.class))
            .execute();
        Map<String, String> newCaps = handle.createQuery("select caps from campaigns where id=?")
                .bind(0, 3)
                .mapTo(QualifiedType.of(STRING_MAP).with(HStore.class))
                .one();
        assertThat(newCaps).isEqualTo(caps);
    }

    @Test
    public void testSqlObjectApi() {
        CampaignDao campaignDao = handle.attach(CampaignDao.class);
        campaignDao.insertCampaign(3, caps);
        assertThat(campaignDao.getCampaignsCaps(3)).isEqualTo(caps);
    }

    @Test
    public void testSqlObjectBindRawMapApi() {
        CampaignDao campaignDao = handle.attach(CampaignDao.class);
        campaignDao.insertCampaignRaw(3, caps);
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
        @SingleValue
        @HStore
        Map<String, String> getCampaignsCaps(@Bind("id") long campaignsId);

        @SqlUpdate("insert into campaigns(id, caps) values (:id, :caps)")
        void insertCampaign(@Bind("id") long campaignsId,
                            @HStore Map<String, String> caps);

        @SqlUpdate("insert into campaigns(id, caps) values (:id, :caps)")
        void insertCampaignRaw(@Bind("id") long campaignsId,
                               @HStore Map caps);

        @SqlUpdate("insert into campaigns(id, caps) values (:id, :caps)")
        int insertCampaignFromMap(@BindMap Map<String, Object> bindings);
    }
}
