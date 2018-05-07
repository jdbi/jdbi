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
package org.jdbi.v3.stringsubstitutor;

/**
 * Convenient semantic factory for common {@link StringSubstitutorTemplateEngine} instances.
 */
public class StringSubstitutorTemplateEngineFactory {
    private StringSubstitutorTemplateEngineFactory() {}

    public static StringSubstitutorTemplateEngine between(char prefix, char suffix) {
        return substitutor -> {
            substitutor.setVariablePrefix(prefix);
            substitutor.setVariableSuffix(suffix);
        };
    }

    public static StringSubstitutorTemplateEngine between(String prefix, String suffix) {
        return substitutor -> {
            substitutor.setVariablePrefix(prefix);
            substitutor.setVariableSuffix(suffix);
        };
    }

    public static StringSubstitutorTemplateEngine between(char prefix, char suffix, char escape) {
        return substitutor -> {
            substitutor.setVariablePrefix(prefix);
            substitutor.setVariableSuffix(suffix);
            substitutor.setEscapeChar(escape);
        };
    }

    public static StringSubstitutorTemplateEngine between(String prefix, String suffix, char escape) {
        return substitutor -> {
            substitutor.setVariablePrefix(prefix);
            substitutor.setVariableSuffix(suffix);
            substitutor.setEscapeChar(escape);
        };
    }
}
