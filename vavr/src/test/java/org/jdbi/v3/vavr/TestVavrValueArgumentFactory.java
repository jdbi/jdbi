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
package org.jdbi.v3.vavr;

import java.lang.reflect.Type;
import java.util.Optional;

import io.vavr.Lazy;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import io.vavr.control.Validation;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.generic.GenericType;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestVavrValueArgumentFactory {
    private static final Type TRY_INTEGER = new GenericType<Try<Integer>>() {}.getType();
    private static final Type OPTION_INTEGER = new GenericType<Option<Integer>>() {}.getType();
    private static final Type LAZY_INTEGER = new GenericType<Lazy<Integer>>() {}.getType();
    private static final Type LAZY_WILDCARD = new GenericType<Lazy<?>>() {}.getType();
    private static final Type EITHER_STRING_INTEGER = new GenericType<Either<String, Integer>>() {}.getType();
    private static final Type EITHER_WILDCARD = new GenericType<Either<?, ?>>() {}.getType();
    private static final Type VALIDATION_STRING_INT = new GenericType<Validation<String, Integer>>() {}.getType();

    private ConfigRegistry configRegistry = new ConfigRegistry();

    private VavrValueArgumentFactory unit = new VavrValueArgumentFactory();

    @Test
    public void testGetNonValueArgumentShouldNotBeEmpty() {
        Optional<Argument> arg = unit.build(OPTION_INTEGER, Option.of(1), configRegistry);

        assertThat(arg).isNotEmpty();
    }

    @Test
    public void testGetArgumentOfNoneShouldNotBeEmpty() {
        Optional<Argument> arg = unit.build(OPTION_INTEGER, Option.none(), configRegistry);

        assertThat(arg).isNotEmpty();
    }

    @Test
    public void testGetLazyArgumentShouldNotBeEmpty() {
        Optional<Argument> arg = unit.build(LAZY_INTEGER, Lazy.of(() -> 1), configRegistry);

        assertThat(arg).isNotEmpty();
    }

    @Test
    public void testGetLazyArgumentInferredShouldNotBeEmpty() {
        Optional<Argument> arg = unit.build(LAZY_WILDCARD, Lazy.of(() -> 1), configRegistry);

        assertThat(arg).isNotEmpty();
    }

    @Test
    public void testGetBadLazyArgumentShouldThrow() {
        Lazy<Object> badEvaluatingLazy = Lazy.of(() -> {
            throw new TestSpecificException();
        });

        assertThatThrownBy(() -> unit.build(LAZY_INTEGER, badEvaluatingLazy, configRegistry))
                .isInstanceOf(TestSpecificException.class);
    }

    @Test
    public void testGetFailedTryArgumentShouldNotBeEmpty() {
        Optional<Argument> arg = unit.build(TRY_INTEGER, Try.failure(new TestSpecificException()), configRegistry);

        assertThat(arg).isNotEmpty();
    }

    @Test
    public void testGetSuccessTryArgumentShouldNotBeEmpty() {
        Optional<Argument> arg = unit.build(TRY_INTEGER, Try.failure(new TestSpecificException()), configRegistry);

        assertThat(arg).isNotEmpty();
    }

    @Test
    public void testGetLeftEitherArgumentShouldNotBeEmpty() {
        Optional<Argument> arg = unit.build(EITHER_STRING_INTEGER, Either.left("error"), configRegistry);

        assertThat(arg).isNotEmpty();
    }

    @Test
    public void testGetRightEitherArgumentShouldNotBeEmpty() {
        Optional<Argument> arg = unit.build(EITHER_STRING_INTEGER, Either.right(1), configRegistry);

        assertThat(arg).isNotEmpty();
    }

    @Test
    public void testGetRightEitherArgumentInferredShouldNotBeEmpty() {
        Optional<Argument> arg = unit.build(EITHER_WILDCARD, Either.right(1), configRegistry);

        assertThat(arg).isNotEmpty();
    }

    @Test
    public void testGetValidValidationArgumentShouldNotBeEmpty() {
        Optional<Argument> arg = unit.build(VALIDATION_STRING_INT, Validation.valid(1), configRegistry);

        assertThat(arg).isNotEmpty();
    }

    @Test
    public void testGetInvalidValidationArgumentShouldNotBeEmpty() {
        Optional<Argument> arg = unit.build(VALIDATION_STRING_INT, Validation.invalid("error"), configRegistry);

        assertThat(arg).isNotEmpty();
    }

    @Test
    public void testGetArgumentForNull() {
        Optional<Argument> arg = unit.build(OPTION_INTEGER, null, configRegistry);

        assertThat(arg).isNotEmpty();
    }

    @Test
    public void testGetArgumentNotPartOfFactoryShouldBeEmpty() {
        Optional<Argument> arg = unit.build(new GenericType<Integer>() {}.getType(), null, configRegistry);

        assertThat(arg).isEmpty();
    }

    private static class TestSpecificException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
