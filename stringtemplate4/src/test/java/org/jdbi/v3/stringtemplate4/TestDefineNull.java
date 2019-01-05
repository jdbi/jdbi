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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestDefineNull {
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule();

    private Handle h;

    private ByteArrayOutputStream err = new ByteArrayOutputStream();
    private PrintStream savedErr;

    @Before
    public void setup() {
        savedErr = System.err;
        System.setErr(new PrintStream(err));
        h = dbRule.getSharedHandle();
        h.setTemplateEngine(new StringTemplateEngine());
    }

    @After
    public void restore() {
        System.setErr(savedErr);
    }

    @Test
    public void testDefineNullDoesntWriteToStderr() {
        assertThat(h.createQuery("select true<if(defined)>broken<endif>")
                .define("defined", null)
                .mapTo(boolean.class)
                .findOnly())
            .isEqualTo(true);
        assertThat(err.toString()).isEmpty();
    }
}
