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
package org.jdbi.v3.stringtemplate4;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.cache.internal.DefaultJdbiCacheStats;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.statement.SqlStatements;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Covers the caching behavior of {@link StringTemplateEngine#parse}. */
public class TestStringTemplateEngineCaching {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2();

    Handle handle;

    @BeforeEach
    void setup() {
        handle = h2Extension.getSharedHandle();
        handle.execute("create table tbl (id int, name varchar)");
        handle.execute("insert into tbl (id, name) values (1, 'a')");
    }

    /** The documented pattern for a render() override: also override parse() to opt out of caching. */
    @Test
    void subclassOptsOutOfCachingToKeepRenderOverride() {
        handle.setTemplateEngine(new StringTemplateEngine() {
            @Override
            public String render(String sql, StatementContext ctx) {
                return super.render(sql, ctx).replace("__TBL__", "tbl");
            }

            @Override
            public Optional<Function<StatementContext, String>> parse(String sql, ConfigRegistry config) {
                return Optional.empty();
            }
        });
        for (int i = 0; i < 3; i++) {
            assertThat(handle.createQuery("select count(*) from __TBL__ where id = <id>")
                    .define("id", 1)
                    .mapTo(Long.class)
                    .one()).isEqualTo(1L);
        }
    }

    @Test
    void statelessEnginesAreEqualByClass() {
        assertThat(new StringTemplateEngine())
                .isEqualTo(new StringTemplateEngine())
                .hasSameHashCodeAs(new StringTemplateEngine());
    }

    /** A fresh engine instance per statement must reuse the shared cache entry, not add new ones. */
    @Test
    void freshEngineInstancesShareOneCacheEntry() {
        String sql = "select count(*) from tbl where id = <id>";
        assertThat(query(sql)).isEqualTo(1L);
        DefaultJdbiCacheStats before = handle.getConfig(SqlStatements.class).cacheStats();
        assertThat(query(sql)).isEqualTo(1L);
        assertThat(query(sql)).isEqualTo(1L);
        DefaultJdbiCacheStats after = handle.getConfig(SqlStatements.class).cacheStats();
        assertThat(after.cacheSize()).isEqualTo(before.cacheSize());
    }

    @Test
    void dynamicIncludeOfMissingTemplateThrowsEveryRender() {
        handle.setTemplateEngine(new StringTemplateEngine());
        for (String tname : List.of("alpha", "beta", "alpha")) {
            assertThatThrownBy(() -> handle.createQuery("select <(tname)()> from tbl")
                    .define("tname", tname)
                    .mapTo(String.class)
                    .one())
                    .isInstanceOf(UnableToExecuteStatementException.class);
        }
    }

    @Test
    void negativeTemplateLookupIsNotRetained() {
        StringTemplateEngine.StatementGroup group = new StringTemplateEngine.StatementGroup();
        assertThat(group.lookupTemplate("missing")).isNull();
        assertThat(group.rawGetTemplate("/missing")).isNull();
    }

    private long query(String sql) {
        return handle.createQuery(sql)
                .setTemplateEngine(new StringTemplateEngine())
                .define("id", 1)
                .mapTo(Long.class)
                .one();
    }
}
