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
package org.jdbi.v3.core.rewriter;

import java.util.ArrayList;
import java.util.List;

class ParsedStatement {
    static final String POSITIONAL_PARAM = "?";

    private String parsedSql;
    private boolean positional = true;
    List<String> params = new ArrayList<>();

    String getParsedSql() {
        return parsedSql;
    }

    void setParsedSql(String parsedSql) {
        this.parsedSql = parsedSql;
    }

    boolean isPositional() {
        return positional;
    }

    void addNamedParam(String name) {
        positional = false;
        params.add(name);
    }

    void addPositionalParam() {
        params.add(POSITIONAL_PARAM);
    }

    List<String> getParams() {
        return params;
    }
}
