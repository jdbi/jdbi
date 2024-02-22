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

import org.jdbi.core.statement.StatementContext;
import org.jdbi.core.statement.TemplateEngine;
import org.jdbi.core.statement.UnableToCreateStatementException;
import org.jdbi.core.statement.UnableToExecuteStatementException;
import org.jdbi.core.config.ConfigRegistry;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STErrorListener;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.misc.STMessage;

/**
 * Rewrites a StringTemplate template, using the attributes on the {@link StatementContext} as template parameters.
 * For configuration, see {@link StringTemplates}.
 */
public class StringTemplateEngine implements TemplateEngine {
    @Override
    public String render(String sql, ConfigRegistry config) {
        STGroup group = new STGroup();
        group.setListener(new ErrorListener(config));
        ST template = new ST(group, sql);

        config.getAttributes().forEach(template::add);

        return template.render();
    }

    static class ErrorListener implements STErrorListener {
        private final ConfigRegistry config;

        ErrorListener(ConfigRegistry config) {
            this.config = config;
        }

        @Override
        public void compileTimeError(STMessage msg) {
            throw new UnableToCreateStatementException("Compiling StringTemplate failed: " + msg, msg.cause, null);
        }

        @Override
        public void runTimeError(STMessage msg) {
            switch (msg.error) {
                case NO_SUCH_PROPERTY:
                    break;
                case NO_SUCH_ATTRIBUTE:
                    if (!config.getConfig(StringTemplates.class).isFailOnMissingAttribute()) {
                        break;
                    }
                // fallthrough
                default:
                    throw new UnableToExecuteStatementException("Executing StringTemplate failed: " + msg, msg.cause, null);
            }
        }

        @Override
        public void IOError(STMessage msg) {
            runTimeError(msg);
        }

        @Override
        public void internalError(STMessage msg) {
            runTimeError(msg);
        }
    }
}
