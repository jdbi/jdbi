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
import io.vavr.NotImplementedError;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import io.vavr.control.Validation;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.generic.GenericType;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestVavrValueArgumentFactory {

    private static final Argument MOCK_ARGUMENT = ((position, statement, ctx) -> new NotImplementedError());

    private VavrValueArgumentFactory unit;

    @Before
    public void setUp() {
        unit = new VavrValueArgumentFactory() {
            @Override
            Optional<Argument> resolveNestedFromConfigured(ConfigRegistry config, Type nestedType, Object nestedValue) {
                return Optional.of(MOCK_ARGUMENT);
            }
        };
    }

    @Test
    public void testGetNonValueArgumentShouldNotBeEmpty() {
        Optional<Argument> arg = unit.build(new GenericType<Option<Integer>>() {}.getType(),
                Option.of(1), null);

        assertThat(arg).isNotEmpty();
    }

    @Test
    public void testGetArgumentOfNoneShouldNotBeEmpty() {
        Optional<Argument> arg = unit.build(new GenericType<Option<Integer>>() {}.getType(),
                Option.none(), null);

        assertThat(arg).isNotEmpty();
    }

    @Test
    public void testGetLazyArgumentShouldNotBeEmpty() {
        Optional<Argument> arg = unit.build(new GenericType<Lazy<Integer>>() {}.getType(),
                Lazy.of(() -> 1), null);

        assertThat(arg).isNotEmpty();
    }

    @Test
    public void testGetLazyArgumentInferredShouldNotBeEmpty() {
        Optional<Argument> arg = unit.build(new GenericType<Lazy<?>>() {}.getType(),
                Lazy.of(() -> 1), null);

        assertThat(arg).isNotEmpty();
    }

    @Test
    public void testGetBadLazyArgumentShouldThrow() {
        Lazy<Object> badEvaluatingLazy = Lazy.of(() -> {
            throw new TestSpecificException();
        });

        assertThatThrownBy(() -> unit.build(new GenericType<Lazy<Integer>>() {}.getType(),
                badEvaluatingLazy, null))
                .isInstanceOf(TestSpecificException.class);
    }

    @Test
    public void testGetFailedTryArgumentShouldNotBeEmpty() {
        Optional<Argument> arg = unit.build(new GenericType<Try<Integer>>() {}.getType(),
                Try.failure(new TestSpecificException()), null);

        assertThat(arg).isNotEmpty();
    }

    @Test
    public void testGetSuccessTryArgumentShouldNotBeEmpty() {
        Optional<Argument> arg = unit.build(new GenericType<Try<Integer>>() {}.getType(),
                Try.failure(new TestSpecificException()), null);

        assertThat(arg).isNotEmpty();
    }

    @Test
    public void testGetLeftEitherArgumentShouldNotBeEmpty() {
        Optional<Argument> arg = unit.build(new GenericType<Either<String, Integer>>() {}.getType(),
                Either.left("error"), null);

        assertThat(arg).isNotEmpty();
    }

    @Test
    public void testGetRightEitherArgumentShouldNotBeEmpty() {
        Optional<Argument> arg = unit.build(new GenericType<Either<String, Integer>>() {}.getType(),
                Either.right(1), null);

        assertThat(arg).isNotEmpty();
    }

    @Test
    public void testGetRightEitherArgumentInferredShouldNotBeEmpty() {
        Optional<Argument> arg = unit.build(new GenericType<Either<?, ?>>() {}.getType(),
                Either.right(1), null);

        assertThat(arg).isNotEmpty();
    }

    @Test
    public void testGetValidValidationArgumentShouldNotBeEmpty() {
        Optional<Argument> arg =
                unit.build(new GenericType<Validation<String, Integer>>() {}.getType(),
                        Validation.valid(1), null);

        assertThat(arg).isNotEmpty();
    }

    @Test
    public void testGetInvalidValidationArgumentShouldNotBeEmpty() {
        Optional<Argument> arg =
                unit.build(new GenericType<Validation<String, Integer>>() {}.getType(),
                        Validation.invalid("error"), null);

        assertThat(arg).isNotEmpty();
    }

    @Test
    public void testGetArgumentNotPartOfFactoryShouldBeEmpty() {
        Optional<Argument> arg = unit.build(new GenericType<Option<Integer>>() {}.getType(),
                1, null);

        assertThat(arg).isEmpty();
    }

    @Test
    public void testGetArgumentNotPartOfFactory2ShouldBeEmpty() {
        Optional<Argument> arg = unit.build(new GenericType<Integer>() {}.getType(),
                null, null);

        assertThat(arg).isEmpty();
    }

    private static class TestSpecificException extends RuntimeException {}

}
