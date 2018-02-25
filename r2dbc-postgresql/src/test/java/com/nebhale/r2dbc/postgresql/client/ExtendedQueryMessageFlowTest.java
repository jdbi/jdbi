/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nebhale.r2dbc.postgresql.client;

import com.nebhale.r2dbc.postgresql.message.backend.BindComplete;
import com.nebhale.r2dbc.postgresql.message.backend.CommandComplete;
import com.nebhale.r2dbc.postgresql.message.backend.NoData;
import com.nebhale.r2dbc.postgresql.message.backend.ParseComplete;
import com.nebhale.r2dbc.postgresql.message.frontend.Bind;
import com.nebhale.r2dbc.postgresql.message.frontend.Close;
import com.nebhale.r2dbc.postgresql.message.frontend.Describe;
import com.nebhale.r2dbc.postgresql.message.frontend.Execute;
import com.nebhale.r2dbc.postgresql.message.frontend.ExecutionType;
import com.nebhale.r2dbc.postgresql.message.frontend.Parse;
import com.nebhale.r2dbc.postgresql.message.frontend.Sync;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;

import static com.nebhale.r2dbc.postgresql.client.TestClient.NO_OP;
import static com.nebhale.r2dbc.postgresql.message.Format.BINARY;
import static com.nebhale.r2dbc.postgresql.util.TestByteBufAllocator.TEST;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public final class ExtendedQueryMessageFlowTest {

    @Test
    public void execute() {
        Flux<Binding> bindings = Flux.just(
            new Binding().add(0, new Parameter(BINARY, 100, TEST.buffer(4).writeInt(200))),
            new Binding().add(0, new Parameter(BINARY, 100, TEST.buffer(4).writeInt(300)))
        );

        Client client = TestClient.builder()
            .expectRequest(
                new Bind("B_0", Collections.singletonList(BINARY), Collections.singletonList(TEST.buffer(4).writeInt(200)), Collections.emptyList(), "test-name"),
                new Describe("B_0", ExecutionType.PORTAL),
                new Execute("B_0", 0),
                new Close("B_0", ExecutionType.PORTAL),
                new Bind("B_1", Collections.singletonList(BINARY), Collections.singletonList(TEST.buffer(4).writeInt(300)), Collections.emptyList(), "test-name"),
                new Describe("B_1", ExecutionType.PORTAL),
                new Execute("B_1", 0),
                new Close("B_1", ExecutionType.PORTAL),
                Sync.INSTANCE)
            .thenRespond(
                BindComplete.INSTANCE, NoData.INSTANCE, new CommandComplete("test", null, null),
                BindComplete.INSTANCE, NoData.INSTANCE, new CommandComplete("test", null, null)
            )
            .build();

        PortalNameSupplier portalNameSupplier = new LinkedList<>(Arrays.asList("B_0", "B_1"))::remove;

        ExtendedQueryMessageFlow
            .execute(bindings, client, portalNameSupplier, "test-name")
            .as(StepVerifier::create)
            .expectNext(BindComplete.INSTANCE, NoData.INSTANCE, new CommandComplete("test", null, null))
            .expectNext(BindComplete.INSTANCE, NoData.INSTANCE, new CommandComplete("test", null, null))
            .verifyComplete();
    }

    @Test
    public void executeNoBindings() {
        assertThatNullPointerException().isThrownBy(() -> ExtendedQueryMessageFlow.execute(null, NO_OP, () -> "", "test-statement"))
            .withMessage("bindings must not be null");
    }

    @Test
    public void executeNoClient() {
        assertThatNullPointerException().isThrownBy(() -> ExtendedQueryMessageFlow.execute(Flux.empty(), null, () -> "", "test-statement"))
            .withMessage("client must not be null");
    }

    @Test
    public void executeNoPortalNameSupplier() {
        assertThatNullPointerException().isThrownBy(() -> ExtendedQueryMessageFlow.execute(Flux.empty(), NO_OP, null, "test-statement"))
            .withMessage("portalNameSupplier must not be null");
    }

    @Test
    public void executeNoStatement() {
        assertThatNullPointerException().isThrownBy(() -> ExtendedQueryMessageFlow.execute(Flux.empty(), NO_OP, () -> "", null))
            .withMessage("statement must not be null");
    }

    @Test
    public void parse() {
        Client client = TestClient.builder()
            .expectRequest(new Parse("test-name", Collections.singletonList(100), "test-query"), new Describe("test-name", ExecutionType.STATEMENT), Sync.INSTANCE)
            .thenRespond(ParseComplete.INSTANCE)
            .build();

        ExtendedQueryMessageFlow
            .parse(client, "test-name", "test-query", Collections.singletonList(100))
            .as(StepVerifier::create)
            .expectNext(ParseComplete.INSTANCE)
            .verifyComplete();
    }

    @Test
    public void parseNoClient() {
        assertThatNullPointerException().isThrownBy(() -> ExtendedQueryMessageFlow.parse(null, "test-name", "test-query", Collections.emptyList()))
            .withMessage("client must not be null");
    }

    @Test
    public void parseNoName() {
        assertThatNullPointerException().isThrownBy(() -> ExtendedQueryMessageFlow.parse(NO_OP, null, "test-query", Collections.emptyList()))
            .withMessage("name must not be null");
    }

    @Test
    public void parseNoQuery() {
        assertThatNullPointerException().isThrownBy(() -> ExtendedQueryMessageFlow.parse(NO_OP, "test-name", null, Collections.emptyList()))
            .withMessage("query must not be null");
    }

    @Test
    public void parseNoTypes() {
        assertThatNullPointerException().isThrownBy(() -> ExtendedQueryMessageFlow.parse(NO_OP, "test-name", "test-query", null))
            .withMessage("types must not be null");
    }

}
