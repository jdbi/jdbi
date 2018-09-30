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
package org.jdbi.v3.spring;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.ColumnMappers;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.RowMappers;
import org.jdbi.v3.core.statement.StatementContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestMapperInstall.MapperConfig.class)
public class TestMapperInstall {

    static class CustomRowType {}
    static class CustomColumnType {}

    @Configuration
    public static class MapperConfig {

        @Bean
        public JdbiFactoryBean jdbiFactory() {
            return new JdbiFactoryBean().setDataSource(new DriverManagerDataSource());
        }

        @Bean
        public RowMapper<CustomRowType> rowMapper() {
            return new CustomRowMapper();
        }

        @Bean
        public ColumnMapper<CustomColumnType> columnMapper() {
            return new CustomColumnMapper();
        }

        // For documentation: this does not work since jdbi cannot infer the type
//        @Bean
//        public ColumnMapper<CustomColumnType> columnMapper() {
//            return (r, columnNumber, ctx) -> null;
//        }
    }

    static class CustomRowMapper implements RowMapper<CustomRowType> {
        @Override
        public CustomRowType map(ResultSet rs, StatementContext ctx) throws SQLException {
            return null;
        }
    }

    static class CustomColumnMapper implements ColumnMapper<CustomColumnType> {
        @Override
        public CustomColumnType map(ResultSet r, int columnNumber, StatementContext ctx) throws SQLException {
            return null;
        }
    }

    @Autowired
    MapperConfig config;

    @Autowired
    Jdbi db;

    @Test
    public void testMappersInstalled() {
        assertThat(db).isNotNull();
        assertThat(db.getConfig().get(RowMappers.class).findFor(CustomRowType.class)).isPresent();
        assertThat(db.getConfig().get(ColumnMappers.class).findFor(CustomColumnType.class)).isPresent();
    }
}
