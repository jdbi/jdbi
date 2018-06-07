package org.jdbi.v3.core.locator.internal;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ClasspathBuilder {
    private static final String DOT = ".";
    private static final String SLASH = "/";

    private final List<String> parts = new ArrayList<>();
    private String extension = null;

    public ClasspathBuilder append(@Nonnull String s) {
        sanitize(s).ifPresent(this::add);
        return this;
    }

    public ClasspathBuilder appendPackage(@Nonnull Package p) {
        return add(p.getName().replace(DOT, SLASH));
    }

    public ClasspathBuilder appendSimpleClassName(@Nonnull Class c) {
        return add(c.getSimpleName());
    }

    public ClasspathBuilder appendMethodName(@Nonnull Method m) {
        return add(m.getName());
    }

    public ClasspathBuilder appendFullyQualifiedClassName(@Nonnull Class c) {
        return appendPackage(c.getPackage()).appendSimpleClassName(c);
    }

    public ClasspathBuilder appendSimpleClassAndMethodName(@Nonnull Method m) {
        return appendSimpleClassName(m.getDeclaringClass()).appendMethodName(m);
    }

    public ClasspathBuilder appendFullyQualifiedMethodName(@Nonnull Method m) {
        return appendFullyQualifiedClassName(m.getDeclaringClass()).appendMethodName(m);
    }

    public ClasspathBuilder appendDotPath(@Nonnull String p) {
        return add(p.replace(DOT, SLASH));
    }

    public ClasspathBuilder setExtension(@Nullable String extension) {
        if (extension != null) {
            String trimmed = extension.trim();
            if (!trimmed.isEmpty()) {
                this.extension = trimmed;
            }
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

        String ext = extension == null ? "" : DOT + extension;
        return String.join(SLASH, parts) + ext;
    }

    /**
     * @return a readable representation of this builder — NOT the path string!
     */
    @Override
    public String toString() {
        return extension == null ? parts.toString() : (parts.toString() + " + ." + extension);
    }

    private ClasspathBuilder add(String s) {
        parts.add(s);
        return this;
    }

    private static Optional<String> sanitize(String path) {
        if (path == null || path.isEmpty() || SLASH.equals(path)) {
            return Optional.empty();
        }

        String sanitized = Arrays.stream(path.split(SLASH))
            .map(part -> part.isEmpty() ? null : part)
            .filter(Objects::nonNull)
            .collect(Collectors.joining(SLASH));

        return Optional.of(sanitized);
    }
}
