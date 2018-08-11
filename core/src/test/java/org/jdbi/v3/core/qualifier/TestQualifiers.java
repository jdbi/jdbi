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
package org.jdbi.v3.core.qualifier;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.jdbi.v3.core.generic.GenericType;
import org.junit.Test;

public class TestQualifiers {
    @Test
    @NVarchar
    public void testNVarcharQualifierConstant() throws Exception {
        NVarchar synthetic = Qualifiers.NVARCHAR;
        NVarchar real = getClass().getMethod("testNVarcharQualifierConstant").getAnnotation(NVarchar.class);

        assertThat(real).isEqualTo(synthetic);
        assertThat(synthetic).isEqualTo(real);
        assertThat(synthetic.hashCode()).isEqualTo(real.hashCode());
        assertThat(synthetic.toString()).isEqualTo(real.toString());
    }

    @Test
    public void testQualifiedType() throws Exception {
        assertThat(QualifiedType.of(String.class, Qualifiers.NVARCHAR))
            .isEqualTo(QualifiedType.of(String.class, Qualifiers.NVARCHAR))
            .hasSameHashCodeAs(QualifiedType.of(String.class, Qualifiers.NVARCHAR))
            .hasToString("@org.jdbi.v3.core.qualifier.NVarchar() java.lang.String");

        assertThat(QualifiedType.of(int.class))
            .isEqualTo(QualifiedType.of(int.class))
            .hasSameHashCodeAs(QualifiedType.of(int.class))
            .hasToString("int");

        assertThat(QualifiedType.of(new GenericType<List<String>>() {}))
            .isEqualTo(QualifiedType.of(new GenericType<List<String>>() {}))
            .hasSameHashCodeAs(QualifiedType.of(new GenericType<List<String>>() {}))
            .hasToString("java.util.List<java.lang.String>");
    }
}
