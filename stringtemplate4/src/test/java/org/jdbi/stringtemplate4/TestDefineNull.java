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
package org.jdbi.stringtemplate4;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;

import org.jdbi.core.Handle;
import org.jdbi.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestDefineNull {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2();

    private Handle h;

    private ByteArrayOutputStream err = new ByteArrayOutputStream();
    private PrintStream savedErr;

    @BeforeEach
    public void setup() {
        savedErr = System.err;
        System.setErr(new PrintStream(err));
        h = h2Extension.getSharedHandle();
        h.setTemplateEngine(new StringTemplateEngine());
    }

    @AfterEach
    public void restore() {
        System.setErr(savedErr);
    }

    @Test
    public void testDefineNullDoesntWriteToStderr() {
        assertThat(h.createQuery("select true<if(defined)>broken<endif>")
                       .define("defined", null)
                       .mapTo(boolean.class)
                       .one())
            .isTrue();
        assertThat(err.toString(Charset.defaultCharset())).isEmpty();
    }
}
