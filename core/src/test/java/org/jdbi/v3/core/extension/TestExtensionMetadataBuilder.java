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
package org.jdbi.v3.core.extension;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestExtensionMetadataBuilder {

    @Test
    public void discoversExtensionMethodsByDefault() throws Exception {
        ExtensionMetadata metadata = ExtensionMetadata.builder(Dao.class).build();

        assertThat(metadata.getExtensionMethods()).containsExactlyInAnyOrder(
                Dao.class.getMethod("first"),
                Dao.class.getMethod("second"));
    }

    @Test
    public void suppliedExtensionMethodsReplaceDiscovery() throws Exception {
        Method first = Dao.class.getMethod("first");

        ExtensionMetadata metadata = ExtensionMetadata.builder(Dao.class)
                .setExtensionTypeMethods(List.of(first))
                .build();

        assertThat(metadata.getExtensionMethods()).containsExactly(first);
    }

    @Test
    public void suppliedStaticMethodsAreSkipped() throws Exception {
        Method first = Dao.class.getMethod("first");
        Method helper = Dao.class.getMethod("helper");

        ExtensionMetadata metadata = ExtensionMetadata.builder(Dao.class)
                .setExtensionTypeMethods(List.of(first, helper))
                .build();

        assertThat(metadata.getExtensionMethods()).containsExactly(first);
    }

    @Test
    public void discoveredAmbiguousMethodsAreRejected() {
        assertThatThrownBy(() -> ExtensionMetadata.builder(AmbiguousDao.class).build())
                .isInstanceOf(UnableToCreateExtensionException.class)
                .hasMessageContaining("ambiguous methods");
    }

    @Test
    public void suppliedAmbiguousMethodsAreRejected() throws Exception {
        List<Method> methods = List.of(
                VersionA.class.getMethod("value"),
                VersionB.class.getMethod("value"));

        assertThatThrownBy(() -> ExtensionMetadata.builder(AmbiguousDao.class)
                .setExtensionTypeMethods(methods)
                .build())
                .isInstanceOf(UnableToCreateExtensionException.class)
                .hasMessageContaining("ambiguous methods");
    }

    @Test
    public void suppliedMethodsAvoidAmbiguityCheckOnDiscoveredMethods() throws Exception {
        Method value = VersionA.class.getMethod("value");

        ExtensionMetadata metadata = ExtensionMetadata.builder(AmbiguousDao.class)
                .setExtensionTypeMethods(List.of(value))
                .build();

        assertThat(metadata.getExtensionMethods()).containsExactly(value);
    }

    public interface Dao {
        String first();

        String second();

        static String helper() {
            return "helper";
        }
    }

    public interface VersionA {
        String value();
    }

    public interface VersionB {
        String value();
    }

    public interface AmbiguousDao extends VersionA, VersionB {}
}
