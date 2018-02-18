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

package com.nebhale.r2dbc.postgresql;

import com.nebhale.r2dbc.postgresql.client.Binding;
import com.nebhale.r2dbc.postgresql.client.Client;
import com.nebhale.r2dbc.postgresql.client.ExtendedQueryMessageFlow;
import com.nebhale.r2dbc.postgresql.client.Parameter;
import com.nebhale.r2dbc.postgresql.client.PortalNameSupplier;
import com.nebhale.r2dbc.postgresql.codec.Codecs;
import com.nebhale.r2dbc.postgresql.message.backend.CloseComplete;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.stream.Stream;

import static com.nebhale.r2dbc.postgresql.client.ExtendedQueryMessageFlow.PARAMETER_SYMBOL;
import static com.nebhale.r2dbc.postgresql.message.Format.BINARY;
import static com.nebhale.r2dbc.postgresql.util.ObjectUtils.requireType;
import static java.util.Objects.requireNonNull;

final class ExtendedQueryPostgresqlStatement implements PostgresqlStatement {

    private final Bindings bindings = new Bindings();

    private final Client client;

    private final Codecs codecs;

    private final PortalNameSupplier portalNameSupplier;

    private final String sql;

    private final StatementCache statementCache;

    ExtendedQueryPostgresqlStatement(Client client, Codecs codecs, PortalNameSupplier portalNameSupplier, String sql, StatementCache statementCache) {
        this.client = requireNonNull(client, "client must not be null");
        this.codecs = requireNonNull(codecs, "codecs must not be null");
        this.portalNameSupplier = requireNonNull(portalNameSupplier, "portalNameSupplier must not be null");
        this.sql = requireNonNull(sql, "sql must not be null");
        this.statementCache = requireNonNull(statementCache, "statementCache must not be null");
    }

    @Override
    public ExtendedQueryPostgresqlStatement add() {
        this.bindings.finish();
        return this;
    }

    @Override
    public ExtendedQueryPostgresqlStatement bind(Object identifier, Object value) {
        requireNonNull(identifier, "identifier must not be null");
        requireType(identifier, String.class, "identifier must be a String");

        return bind(getIndex((String) identifier), value);
    }

    @Override
    public ExtendedQueryPostgresqlStatement bind(Integer index, Object value) {
        requireNonNull(index, "index must not be null");

        if (value instanceof Optional) {
            return bind(index, ((Optional<?>) value).orElse(null));
        }

        this.bindings.getCurrent().add(index, this.codecs.encode(value));

        return this;
    }

    @Override
    public ExtendedQueryPostgresqlStatement bindNull(Object identifier, Object type) {
        requireNonNull(identifier, "identifier must not be null");
        requireType(identifier, String.class, "identifier must be a String");
        requireNonNull(type, "type must not be null");
        requireType(type, Integer.class, "type must be a Integer");

        bindNull(getIndex((String) identifier), (Integer) type);
        return this;
    }

    @Override
    public Flux<PostgresqlResult> execute() {
        return this.statementCache.getName(this.bindings.first(), this.sql)
            .flatMapMany(name -> ExtendedQueryMessageFlow
                .execute(Flux.fromStream(this.bindings.stream()), this.client, this.portalNameSupplier, name))
            .windowUntil(CloseComplete.class::isInstance)
            .map(messages -> PostgresqlResult.toResult(this.codecs, messages));
    }

    @Override
    public String toString() {
        return "ExtendedQueryPostgresqlStatement{" +
            "bindings=" + this.bindings +
            ", client=" + this.client +
            ", codecs=" + this.codecs +
            ", portalNameSupplier=" + this.portalNameSupplier +
            ", sql='" + this.sql + '\'' +
            ", statementCache=" + this.statementCache +
            '}';
    }

    static boolean supports(String sql) {
        requireNonNull(sql, "sql must not be null");

        return !sql.trim().isEmpty() && !sql.contains(";") && PARAMETER_SYMBOL.matcher(sql).matches();
    }

    Binding getCurrentBinding() {
        return this.bindings.getCurrent();
    }

    private void bindNull(Integer index, Integer type) {
        this.bindings.getCurrent().add(index, new Parameter(BINARY, type, null));
    }

    private int getIndex(String identifier) {
        Matcher matcher = PARAMETER_SYMBOL.matcher(identifier);

        if (!matcher.find()) {
            throw new IllegalArgumentException(String.format("Identifier '%s' is not a valid identifier. Should be of the pattern '%s'.", identifier, PARAMETER_SYMBOL.pattern()));
        }

        return Integer.parseInt(matcher.group(1)) - 1;
    }

    private static final class Bindings {

        private final List<Binding> bindings = new ArrayList<>();

        private Binding current;

        @Override
        public String toString() {
            return "Bindings{" +
                "bindings=" + this.bindings +
                ", current=" + this.current +
                '}';
        }

        private void finish() {
            this.current = null;
        }

        private Binding first() {
            return this.bindings.stream().findFirst().orElseThrow(() -> new IllegalStateException("No parameters have been bound"));
        }

        private Binding getCurrent() {
            if (this.current == null) {
                this.current = new Binding();
                this.bindings.add(this.current);
            }

            return this.current;
        }

        private Stream<Binding> stream() {
            return this.bindings.stream();
        }

    }

}
