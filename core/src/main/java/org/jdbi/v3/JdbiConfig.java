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
package org.jdbi.v3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jdbi.v3.rewriter.ColonPrefixStatementRewriter;
import org.jdbi.v3.rewriter.StatementRewriter;

class JdbiConfig {
    static JdbiConfig copyOf(JdbiConfig original) {
        return new JdbiConfig(original);
    }

    final Map<String, Object> statementAttributes;

    final ArgumentRegistry argumentRegistry;
    final MappingRegistry mappingRegistry;
    final CollectorFactoryRegistry collectorRegistry;
    final ExtensionRegistry extensionRegistry;

    volatile StatementRewriter statementRewriter;
    volatile TimingCollector timingCollector;

    JdbiConfig() {
        statementAttributes = new ConcurrentHashMap<>();

        argumentRegistry = new ArgumentRegistry();
        mappingRegistry = new MappingRegistry();
        collectorRegistry = new CollectorFactoryRegistry();
        extensionRegistry = new ExtensionRegistry();

        statementRewriter = new ColonPrefixStatementRewriter();
        timingCollector = TimingCollector.NOP_TIMING_COLLECTOR;
    }

    private JdbiConfig(JdbiConfig that) {
        this.statementAttributes = new ConcurrentHashMap<>(that.statementAttributes);

        this.argumentRegistry = ArgumentRegistry.copyOf(that.argumentRegistry);
        this.mappingRegistry = MappingRegistry.copyOf(that.mappingRegistry);
        this.collectorRegistry = CollectorFactoryRegistry.copyOf(that.collectorRegistry);
        this.extensionRegistry = ExtensionRegistry.copyOf(that.extensionRegistry);

        this.statementRewriter = that.statementRewriter;
        this.timingCollector = that.timingCollector;
    }
}
