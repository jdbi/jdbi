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
package org.jdbi.v3.core.locator.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ClasspathBuilder {
    private static final String DOT = ".";
    private static final String SLASH = "/";

    // a part can consist of already slash-concatenated parts
    // preserving combined inputs can help see the bigger picture in debugging
    private final List<String> parts = new ArrayList<>();
    private String extension = null;

    public ClasspathBuilder setExtension(@Nullable String extension) {
        if (extension != null) {
            String trimmed = extension.trim();
            if (!trimmed.isEmpty()) {
                this.extension = trimmed;
            }
        }
        return this;
    }

    // org.foo.Bar$Inner -> org/foo/Bar$Inner
    public ClasspathBuilder appendFullyQualifiedClassName(@Nonnull Class clazz) {
        return appendDotPath(clazz.getName());
    }

    // com.google.guava -> com/google/guava
    public ClasspathBuilder appendDotPath(@Nonnull String path) {
        return appendVerbatim(path.replace(DOT, SLASH));
    }

    // because sometimes you just don't have fancy data structures or patterns to work on
    public ClasspathBuilder appendVerbatim(@Nonnull String s) {
        return addCarefully(s);
    }

    private ClasspathBuilder addCarefully(@Nullable String part) {
        String clean = sanitize(part);
        if (clean != null) {
            parts.add(clean);
        }
        return this;
    }

    /**
     * @return the current path string
     */
    public String build() {
        if (parts.isEmpty()) {
            throw new IllegalStateException("specify path parts before building the path");
        }

        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            sb.append(part).append(SLASH);
        }
        sb.deleteCharAt(sb.length() - 1);
        if (extension != null) {
            sb.append(DOT).append(extension);
        }
        return sb.toString();
    }

    /**
     * @return a readable representation of this builder — NOT the path string!
     */
    @Override
    public String toString() {
        return extension == null
            ? parts.toString()
            : parts.toString() + " + ." + extension;
    }

    @Nullable
    private static String sanitize(String path) {
        // these do not add levels to a path, so ignore them
        if (path == null || path.isEmpty() || SLASH.equals(path)) {
            return null;
        }

        // filter out common beauty flaws like double slashes or whitespace edges
        String sanitized = Arrays.stream(path.split(Pattern.quote(SLASH)))
            .map(String::trim)
            .filter(part -> !part.isEmpty())
            .collect(Collectors.joining(SLASH));

        return sanitized.isEmpty() ? null : sanitized;
    }
}
