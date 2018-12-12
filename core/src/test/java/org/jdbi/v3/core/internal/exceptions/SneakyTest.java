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
package org.jdbi.v3.core.internal.exceptions;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SneakyTest {
    @Test
    public void errorThrownDirectly() {
        Error error = new OutOfMemoryError();

        assertThatThrownBy(() -> Sneaky.throwAnyway(error))
            .isSameAs(error);
    }

    @Test
    public void runtimeExceptionThrownDirectly() {
        RuntimeException rex = new IllegalArgumentException();

        assertThatThrownBy(() -> Sneaky.throwAnyway(rex))
            .isSameAs(rex);
    }

    @Test
    public void ioExceptionProperlyWrapped() {
        IOException ioex = new IOException();

        assertThatThrownBy(() -> Sneaky.throwAnyway(ioex))
            .isInstanceOf(UncheckedIOException.class)
            .hasCause(ioex);
    }

    @Test
    public void genericCheckedExceptionThrownDirectly() {
        Exception ex = new SQLException();

        assertThatThrownBy(() -> Sneaky.throwAnyway(ex))
            .isSameAs(ex);
    }

    @Test
    public void invocationTargetExceptionIsUnwrapped() {
        Throwable cause = new OutOfMemoryError();
        InvocationTargetException ite = new InvocationTargetException(cause);

        assertThatThrownBy(() -> Sneaky.throwAnyway(ite))
            .isSameAs(cause);
    }
}
