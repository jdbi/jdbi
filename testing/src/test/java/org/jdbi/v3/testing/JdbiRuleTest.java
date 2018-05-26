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
package org.jdbi.v3.testing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import javax.sql.DataSource;

import org.h2.jdbcx.JdbcConnectionPool;
import org.jdbi.v3.core.Jdbi;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class JdbiRuleTest {
  @Test
  public void migrateWithFlywayDefaultLocation() throws Throwable {
    JdbiRule rule = JdbiRule.embeddedPostgres().migrateWithFlyway();

    Statement statement =
        new Statement() {
          @Override
          public void evaluate() {
            assertThat(
                    rule.getHandle()
                        .select("select value from standard_migration_location")
                        .mapTo(String.class)
                        .findOnly())
                .isEqualTo("inserted in migration script in the default location");
          }
        };

    rule.apply(statement, Description.EMPTY).evaluate();
  }

  @Test
  public void migrateWithFlywayCustomLocation() throws Throwable {
    JdbiRule rule = JdbiRule.embeddedPostgres().migrateWithFlyway("custom/migration/location");

    Statement statement =
        new Statement() {
          @Override
          public void evaluate() {
            assertThat(
                    rule.getHandle()
                        .select("select value from custom_migration_location")
                        .mapTo(String.class)
                        .findOnly())
                .isEqualTo("inserted in migration script in a custom location");
          }
        };

    rule.apply(statement, Description.EMPTY).evaluate();
  }

  @Test
  public void subclassOverridingCreateJdbi() {
    new JdbiRuleOverrideCreateJdbi();
  }

  @Test
  public void migrateWithFlywayMustOverrideCreateDataSource() throws Throwable {
    JdbiRule rule = new JdbiRuleOverrideCreateJdbi().migrateWithFlyway();

    Statement statement =
        new Statement() {
          @Override
          public void evaluate() {}
        };

    assertThatThrownBy(() -> rule.apply(statement, Description.EMPTY).evaluate())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  static class JdbiRuleOverrideCreateJdbi extends JdbiRule {
    @Override
    protected Jdbi createJdbi() {
      return Jdbi.create("jdbc:h2:mem:" + UUID.randomUUID());
    }
  }

  @Test
  public void subclassOverridingCreateDataSource() {
    new JdbiRule() {
      @Override
      protected DataSource createDataSource() {
        return JdbcConnectionPool.create("jdbc:h2:mem:" + UUID.randomUUID(), "", "");
      }
    };
  }

  @Test
  public void subclassOverridingCreateJdbiAndCreateDataSource() {
    new JdbiRule() {
      @Override
      protected Jdbi createJdbi() {
        return Jdbi.create(createDataSource());
      }

      @Override
      protected DataSource createDataSource() {
        return JdbcConnectionPool.create("jdbc:h2:mem:" + UUID.randomUUID(), "", "");
      }
    };
  }

  @Test
  public void subclassNoOverrides() {
    assertThatThrownBy(() -> new JdbiRule() {}).isInstanceOf(IllegalStateException.class);
  }
}
