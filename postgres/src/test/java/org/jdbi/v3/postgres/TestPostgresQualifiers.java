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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class TestPostgresQualifiers {
    @Test
    @HStore
    public void testHStoreQualifierConstant() throws Exception {
        HStore synthetic = PostgresQualifiers.hStore();
        HStore real = getClass().getMethod("testHStoreQualifierConstant").getAnnotation(HStore.class);

        assertThat(real).isEqualTo(synthetic);
        assertThat(synthetic).isEqualTo(real);
        assertThat(synthetic.hashCode()).isEqualTo(real.hashCode());
        assertThat(synthetic.toString()).isEqualTo(real.toString());
    }

    @Test
    @MacAddr
    public void testMacAddrQualifierConstant() throws Exception {
        MacAddr synthetic = PostgresQualifiers.macAddr();
        MacAddr real = getClass().getMethod("testMacAddrQualifierConstant").getAnnotation(MacAddr.class);

        assertThat(real).isEqualTo(synthetic);
        assertThat(synthetic).isEqualTo(real);
        assertThat(synthetic.hashCode()).isEqualTo(real.hashCode());
        assertThat(synthetic.toString()).isEqualTo(real.toString());
    }
}
