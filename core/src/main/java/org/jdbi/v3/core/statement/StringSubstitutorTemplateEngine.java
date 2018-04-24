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
package org.jdbi.v3.core.statement;

import org.apache.commons.text.StringSubstitutor;

public class StringSubstitutorTemplateEngine implements TemplateEngine {
	private final String prefix, suffix;
	private final Character escape;

	public StringSubstitutorTemplateEngine() {
	    this(null, null, null);
    }

	public StringSubstitutorTemplateEngine(String prefix, String suffix, Character escape) {
		this.prefix = prefix;
		this.suffix = suffix;
		this.escape = escape;
	}

	@Override
	public String render(String template, StatementContext ctx) {
		StringSubstitutor substitutor = new StringSubstitutor(ctx.getAttributes());

		if (prefix != null) {
			substitutor.setVariablePrefix(prefix);
		}
		if (suffix != null) {
			substitutor.setVariableSuffix(suffix);
		}
		if (escape != null) {
			substitutor.setEscapeChar(escape);
		}

		return substitutor.replace(template);
	}

	public static class Builder {
		private String prefix = "${", suffix = "}";
		private Character escape = '$';

		public Builder withPrefix(String prefix) {
			this.prefix = prefix;
			return this;
		}

		public Builder withSuffix(String suffix) {
			this.suffix = suffix;
			return this;
		}

		public Builder withEscape(Character escape) {
			this.escape = escape;
			return this;
		}

		public StringSubstitutorTemplateEngine build() {
			return new StringSubstitutorTemplateEngine(prefix, suffix, escape);
		}
	}
}
