package org.jdbi.core.statement;
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

import org.jdbi.stringtemplate4.StringTemplateEngine;
import org.jdbi.testing.junit.JdbiExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestDefineNamedBindingsST {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2();

    @Test
    public void testDefineBoolean() {
        h2Extension.getSharedHandle().setTemplateEngine(new StringTemplateEngine());
        assertThat(
            h2Extension.getSharedHandle().createQuery("select <a> from values(:a) <if(b)>where false=:b<endif>")
                .defineNamedBindings()
                .bindBean(new DefinedBean())
                .mapTo(boolean.class)
                .one())
            .isTrue();
    }

    public static class DefinedBean {
        public String getA() {
            return "x";
        }

        public Integer getB() {
            return null;
        }
    }
}
