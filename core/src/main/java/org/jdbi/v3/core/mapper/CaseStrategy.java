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
package org.jdbi.v3.core.mapper;

import java.util.Locale;
import java.util.function.UnaryOperator;

import org.jdbi.v3.meta.Beta;

/**
 * Strategies for comparing case sensitive strings.
 */
@Beta
public enum CaseStrategy implements UnaryOperator<String> {
    /**
     * No case sensitivity.
     */
    NOP {
        @Override
        public String apply(String t) {
            return t;
        }
    },
    /**
     * All strings to lowercase in root locale.
     */
    LOWER {
        @Override
        public String apply(String t) {
            return t.toLowerCase(Locale.ROOT);
        }
    },
    /**
     * All strings to uppercase in root locale.
     */
    UPPER {
        @Override
        public String apply(String t) {
            return t.toUpperCase(Locale.ROOT);
        }
    },
    /**
     * All strings to lowercase in system locale.
     */
    LOCALE_LOWER {
        @Override
        public String apply(String t) {
            return t.toLowerCase();
        }
    },
    /**
     * All strings to uppercase in system locale.
     */
    LOCALE_UPPER {
        @Override
        public String apply(String t) {
            return t.toUpperCase();
        }
    },
}
