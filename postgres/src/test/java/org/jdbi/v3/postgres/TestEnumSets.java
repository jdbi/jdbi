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

import org.jdbi.v3.core.PreparedBatch;
import org.jdbi.v3.core.util.GenericType;
import org.jdbi.v3.sqlobject.SqlQuery;
import org.jdbi.v3.sqlobject.SqlUpdate;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TestEnumSets {

    private static final GenericType<EnumSet<Platform>> PLATFORM_SET = new GenericType<EnumSet<Platform>>() {
    };

    @ClassRule
    public static PostgresDbRule db = new PostgresDbRule();

    private VideoDao videoDao;

    @Before
    public void setupDbi() throws Exception {
        db.getSharedHandle().useTransaction((h, status) -> {
            h.execute("drop table if exists videos");
            h.execute("create table videos (id int primary key, supported_platforms bit(5))");
            PreparedBatch batch = h.prepareBatch("insert into videos(id, supported_platforms) values (:id,:supported_platforms::varbit)");
            batch.add()
                    .bind("id", 0)
                    .bindByType("supported_platforms", EnumSet.of(Platform.IOS, Platform.ANDROID, Platform.WEB), PLATFORM_SET);
            batch.add()
                    .bind("id", 1)
                    .bindByType("supported_platforms", EnumSet.of(Platform.SMART_TV), PLATFORM_SET);
            batch.add()
                    .bind("id", 2)
                    .bindByType("supported_platforms", EnumSet.of(Platform.ANDROID, Platform.STB), PLATFORM_SET);
            batch.add()
                    .bind("id", 3)
                    .bindByType("supported_platforms", EnumSet.of(Platform.IOS, Platform.WEB), PLATFORM_SET);
            batch.add()
                    .bind("id", 4)
                    .bindByType("supported_platforms", EnumSet.noneOf(Platform.class), PLATFORM_SET);
            batch.add()
                    .bind("id", 5)
                    .bindByType("supported_platforms", null, PLATFORM_SET);
            batch.execute();
        });
        videoDao = db.getSharedHandle().attach(VideoDao.class);
    }

    @Test
    public void testInserts() {
        videoDao.insert(6, EnumSet.of(Platform.IOS, Platform.ANDROID));
        assertThat(getSupportedPlatforms(6)).containsExactly(Platform.ANDROID, Platform.IOS);
    }

    @Test
    public void testInsertsEmpty() {
        videoDao.insert(7, EnumSet.noneOf(Platform.class));
        assertThat(getSupportedPlatforms(7)).isEmpty();
    }

    @Test
    public void testInsertsNull() {
        videoDao.insert(8, null);
        assertThat(getSupportedPlatforms(8)).isNull();
    }

    @Test
    public void testReads() {
        EnumSet<Platform> supportedPlatforms = videoDao.getSupportedPlatforms(0);
        assertThat(supportedPlatforms).containsOnly(Platform.ANDROID, Platform.IOS, Platform.WEB);
    }

    @Test
    public void testReadsEmpty() {
        EnumSet<Platform> supportedPlatforms = videoDao.getSupportedPlatforms(4);
        assertThat(supportedPlatforms).isEmpty();
    }

    @Test
    public void testReadsNull() {
        EnumSet<Platform> supportedPlatforms = videoDao.getSupportedPlatforms(5);
        assertThat(supportedPlatforms).isNull();
    }

    @Test
    public void testBitwiseWorksForNoneElements() {
        List<Integer> notNullVideos = videoDao.getSupportedVideosOnPlatforms(EnumSet.noneOf(Platform.class));
        assertThat(notNullVideos).containsExactly(0, 1, 2, 3, 4);
    }

    @Test
    public void testBitwiseWorksForOneElement() {
        List<Integer> stbVideos = videoDao.getSupportedVideosOnPlatforms(EnumSet.of(Platform.STB));
        assertThat(stbVideos).containsOnlyOnce(2);
    }

    @Test
    public void testBitwiseWorksForSeveralElements() {
        List<Integer> webIosVideos = videoDao.getSupportedVideosOnPlatforms(EnumSet.of(Platform.WEB, Platform.IOS));
        assertThat(webIosVideos).containsExactly(0, 3);
    }

    @Test
    public void testBitwiseAdditionWorks() {
        videoDao.addPlatforms(1, EnumSet.of(Platform.IOS, Platform.ANDROID));
        EnumSet<Platform> supportedPlatforms = getSupportedPlatforms(1);
        assertThat(supportedPlatforms).containsExactly(Platform.ANDROID, Platform.IOS, Platform.SMART_TV);
    }

    @Test
    public void testBitwiseRemovingWorks() {
        videoDao.removePlatforms(0, EnumSet.of(Platform.IOS, Platform.ANDROID, Platform.SMART_TV));
        EnumSet<Platform> supportedPlatforms = getSupportedPlatforms(0);
        assertThat(supportedPlatforms).containsOnlyOnce(Platform.WEB);
    }

    @Test
    public void testAmountPlatforms(){
        int amount = videoDao.getAmountOfSupportedPlatforms(0);
        assertThat(amount).isEqualTo(3);
    }

    private EnumSet<Platform> getSupportedPlatforms(int id) {
        return db.getSharedHandle()
                .createQuery("select supported_platforms from videos where id=:id")
                .bind("id", id)
                .mapTo(PLATFORM_SET)
                .findOnly();
    }

    public interface VideoDao {

        @SqlUpdate("insert into videos(id, supported_platforms) values (:id, :platforms::varbit)")
        void insert(int id, EnumSet<Platform> platforms);

        @SqlQuery("select supported_platforms from videos where id=:id")
        EnumSet<Platform> getSupportedPlatforms(int id);

        @SqlQuery("select id from videos " +
                "where (supported_platforms & :platforms::varbit) = :platforms::varbit " +
                "order by id")
        List<Integer> getSupportedVideosOnPlatforms(EnumSet<Platform> platforms);
        @SqlUpdate("update videos " +
                "set supported_platforms = (supported_platforms | :platforms::varbit) " +
                "where id=:id")
        void addPlatforms(int id, EnumSet<Platform> platforms);

        @SqlUpdate("update videos " +
                "set supported_platforms = (supported_platforms & ~:platforms::varbit) " +
                "where id=:id")
        void removePlatforms(int id, EnumSet<Platform> platforms);

        @SqlQuery("select length(replace(supported_platforms::varchar, '0', '')) from videos " +
                "where id=:id")
        int getAmountOfSupportedPlatforms(int id);
    }

    public enum Platform {
        ANDROID,
        IOS,
        SMART_TV,
        STB,
        WEB
    }
}
