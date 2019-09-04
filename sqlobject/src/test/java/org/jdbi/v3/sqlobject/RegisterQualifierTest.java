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

import java.util.Optional;

import javax.annotation.Nonnull;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.ColumnMappers;
import org.jdbi.v3.core.mapper.NoSuchMapperException;
import org.jdbi.v3.core.mapper.QualifiedColumnMapperFactory;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.rule.SqliteDatabaseRule;
import org.jdbi.v3.sqlobject.config.RegisterQualifier;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RegisterQualifierTest {
    private static final QualifiedType<String> NONNULL_STRING = QualifiedType.of(String.class).with(Nonnull.class);

    @Rule
    public SqliteDatabaseRule db = new SqliteDatabaseRule().withPlugin(new SqlObjectPlugin());

    private Jdbi jdbi;

    @Before
    public void before() {
        jdbi = db.getJdbi();
    }

    @Test
    public void testWithoutEnforcerRegistered() {
        assertThatThrownBy(() -> jdbi.withHandle(h -> h.createQuery("ascii shrug here").mapTo(NONNULL_STRING)))
            .isInstanceOf(NoSuchMapperException.class);
    }

    @Test
    public void testWithEnforcerRegistered() {
        jdbi.registerColumnMapper(new NonNullEnforcer());
        EnforcedDao dao = jdbi.onDemand(EnforcedDao.class);

        assertThatThrownBy(() -> dao.echo(null))
            .isInstanceOf(NonNullEnforcer.IllegalNull.class);

        String abc = "abc";
        assertThat(dao.echo(abc))
            .isEqualTo(abc);

        assertThat(jdbi.onDemand(LaxDao.class).echo(null))
            .isNull();
    }

    public interface LaxDao {
        @SqlQuery("select :wat")
        @Nonnull
        String echo(String wat);
    }

    @RegisterQualifier(Nonnull.class)
    public interface EnforcedDao {
        @SqlQuery("select :wat")
        @Nonnull
        String echo(String wat);
    }

    public static class NonNullEnforcer implements QualifiedColumnMapperFactory {
        /**
         * because NPE could be something else
         */
        private static class IllegalNull extends RuntimeException {
            private static final long serialVersionUID = 1L;
        }

        @Override
        public Optional<ColumnMapper<?>> build(QualifiedType<?> type, ConfigRegistry config) {
            if (!type.hasQualifier(Nonnull.class)) {
                return Optional.empty();
            }

            return config.get(ColumnMappers.class)
                .findFor(type.remove(Nonnull.class))
                .map(original -> (ColumnMapper<?>) (r, columnNumber, ctx) -> {
                    Object plain = original.map(r, columnNumber, ctx);
                    if (plain == null) {
                        throw new IllegalNull();
                    }
                    return plain;
                });
        }
    }
}
