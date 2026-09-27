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
package org.jdbi.v3.core.mapper.reflect;

import java.util.List;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.jdbi.v3.core.mapper.immutables.JdbiImmutables;
import org.jdbi.v3.core.mapper.reflect.nonpublic.IdName;
import org.jdbi.v3.core.mapper.reflect.nonpublic.NonPublicTypes;
import org.jdbi.v3.core.mapper.reflect.nonpublic.PrivateConstructorBean;
import org.jdbi.v3.core.statement.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.jdbi.v3.core.junit5.H2DatabaseExtension.USERS_INITIALIZER;

class NonPublicTypeAccessTest {

    private static final String SELECT_NAME = "SELECT name FROM users WHERE id = :id";
    private static final String SELECT_USERS = "SELECT id, name FROM users ORDER BY id";
    private static final String NOT_ACCESSIBLE = "Jdbi can not access";

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance().withInitializer(USERS_INITIALIZER);

    private Jdbi jdbi;

    @BeforeEach
    void setUp() {
        jdbi = h2Extension.getJdbi();
    }

    @Test
    void bindsBeanOfNonPublicClass() {
        try (Handle handle = jdbi.open()) {
            assertThat(handle.createQuery(SELECT_NAME)
                    .bindBean(NonPublicTypes.hiddenBean(2, "unused"))
                    .mapTo(String.class)
                    .one())
                .isEqualTo("Bob");
        }
    }

    @Test
    void bindsBeanWithGetterFromNonPublicSuperclass() {
        try (Handle handle = jdbi.open()) {
            assertThat(handle.createQuery(SELECT_NAME)
                    .bindBean(NonPublicTypes.visibleBean(1))
                    .mapTo(String.class)
                    .one())
                .isEqualTo("Alice");
        }
    }

    @Test
    void mapsBeanOfNonPublicClass() {
        try (Handle handle = jdbi.open()) {
            assertUsers(handle.createQuery(SELECT_USERS).mapToBean(NonPublicTypes.HIDDEN_BEAN).list());
        }
    }

    @Test
    void mapsBeanWithPrivateConstructor() {
        try (Handle handle = jdbi.open()) {
            assertUsers(handle.createQuery(SELECT_USERS).mapToBean(PrivateConstructorBean.class).list());
        }
    }

    @Test
    void mapsNonPublicRecord() {
        try (Handle handle = jdbi.open()) {
            assertUsers(handle.createQuery(SELECT_USERS).map(ConstructorMapper.of(NonPublicTypes.HIDDEN_RECORD)).list());
        }
    }

    @Test
    void mapsNonPublicConstructor() {
        try (Handle handle = jdbi.open()) {
            assertUsers(handle.createQuery(SELECT_USERS).map(ConstructorMapper.of(NonPublicTypes.HIDDEN_CONSTRUCTED)).list());
        }
    }

    @Test
    void mapsNonPublicFactoryMethod() {
        try (Handle handle = jdbi.open()) {
            assertUsers(handle.createQuery(SELECT_USERS).map(ConstructorMapper.of(NonPublicTypes.HIDDEN_FACTORY_MADE)).list());
        }
    }

    @Test
    void bindsAndMapsNonPublicImmutable() {
        jdbi.getConfig(JdbiImmutables.class).registerImmutable(NonPublicTypes.HIDDEN_IMMUTABLE);

        try (Handle handle = jdbi.open()) {
            assertThat(handle.createQuery(SELECT_NAME)
                    .bindPojo(NonPublicTypes.hiddenImmutable(2, "unused"))
                    .mapTo(String.class)
                    .one())
                .isEqualTo("Bob");

            assertUsers(handle.createQuery(SELECT_USERS).mapTo(NonPublicTypes.HIDDEN_IMMUTABLE).list());
        }
    }

    @Test
    void disabledStrategyRejectsBeanOfNonPublicClass() {
        jdbi.getConfig(ReflectionMappers.class).disableAccessibleObjectStrategy();

        try (Handle handle = jdbi.open();
            Query query = handle.createQuery(SELECT_NAME)) {
            assertThatThrownBy(() -> query.bindBean(NonPublicTypes.hiddenBean(2, "unused")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(NOT_ACCESSIBLE);
        }
    }

    @Test
    void disabledStrategyNamesAccessProblemForBeanConstructor() {
        jdbi.getConfig(ReflectionMappers.class).disableAccessibleObjectStrategy();

        try (Handle handle = jdbi.open();
            Query query = handle.createQuery(SELECT_USERS)) {
            assertThatThrownBy(() -> query.mapToBean(PrivateConstructorBean.class).list())
                .isInstanceOf(NoSuchMethodException.class)
                .cause()
                .hasMessageContaining(NOT_ACCESSIBLE);
        }
    }

    @Test
    void strategyOnHandleDoesNotChangeCachedAccess() {
        try (Handle handle = jdbi.open()) {
            handle.createQuery(SELECT_NAME).bindBean(NonPublicTypes.hiddenBean(1, "unused")).mapTo(String.class).one();
        }

        try (Handle handle = jdbi.open()) {
            handle.getConfig(ReflectionMappers.class).disableAccessibleObjectStrategy();

            assertThat(handle.createQuery(SELECT_NAME)
                    .bindBean(NonPublicTypes.hiddenBean(2, "unused"))
                    .mapTo(String.class)
                    .one())
                .isEqualTo("Bob");
        }
    }

    @Test
    void disabledStrategyRejectsNonPublicConstructorOnFirstUse() {
        jdbi.getConfig(ReflectionMappers.class).disableAccessibleObjectStrategy();
        var mapper = ConstructorMapper.of(NonPublicTypes.HIDDEN_CONSTRUCTED);

        try (Handle handle = jdbi.open();
            Query query = handle.createQuery(SELECT_USERS)) {
            assertThatThrownBy(() -> query.map(mapper).list())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(NOT_ACCESSIBLE);
        }
    }

    @Test
    void forcedAccessDoesNotReachOtherJdbiInstances() {
        try (Handle handle = jdbi.open()) {
            handle.createQuery(SELECT_NAME).bindBean(NonPublicTypes.hiddenBean(1, "unused")).mapTo(String.class).one();
        }

        Jdbi restricted = Jdbi.create(h2Extension.getUri());
        restricted.getConfig(ReflectionMappers.class).disableAccessibleObjectStrategy();

        try (Handle handle = restricted.open();
            Query query = handle.createQuery(SELECT_NAME)) {
            assertThatThrownBy(() -> query.bindBean(NonPublicTypes.hiddenBean(1, "unused")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(NOT_ACCESSIBLE);
        }
    }

    @Test
    void strategySetAfterImmutableRegistrationApplies() {
        jdbi.getConfig(JdbiImmutables.class).registerImmutable(NonPublicTypes.HIDDEN_IMMUTABLE);
        jdbi.getConfig(ReflectionMappers.class).disableAccessibleObjectStrategy();

        try (Handle handle = jdbi.open();
            Query query = handle.createQuery(SELECT_USERS)) {
            assertThatThrownBy(() -> query.mapTo(NonPublicTypes.HIDDEN_IMMUTABLE).list())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(NOT_ACCESSIBLE);
        }
    }

    private static void assertUsers(List<?> users) {
        assertThat(users)
            .map(IdName.class::cast)
            .extracting(IdName::id, IdName::name)
            .containsExactly(
                tuple(1L, "Alice"),
                tuple(2L, "Bob"));
    }
}
