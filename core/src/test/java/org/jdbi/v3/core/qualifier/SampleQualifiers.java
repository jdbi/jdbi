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

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

class SampleQualifiers {
    private SampleQualifiers() {}

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    @interface Foo {
        int value();
    }

    public static Foo foo(int value) {
        return new Foo() {
            @Override
            public int value() {
                return value;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return Foo.class;
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof Foo && ((Foo) obj).value() == value();
            }

            @Override
            public int hashCode() {
                return value;
            }

            @Override
            public String toString() {
                return "@org.jdbi.v3.core.qualifier.SampleQualifiers.Foo(value=" + value + ")";
            }
        };
    }

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    @interface Bar {
        String value();
    }

    public static Bar bar(String value) {
        return new Bar() {
            @Override
            public String value() {
                return value;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return Bar.class;
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof Bar && ((Bar) obj).value().equals(value());
            }

            @Override
            public int hashCode() {
                return value.hashCode();
            }

            @Override
            public String toString() {
                return "@org.jdbi.v3.core.qualifier.SampleQualifiers.Bar(value=" + value + ")";
            }
        };
    }

}
