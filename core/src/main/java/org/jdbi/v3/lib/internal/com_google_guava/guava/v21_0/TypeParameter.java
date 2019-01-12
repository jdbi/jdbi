/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.jdbi.v3.lib.internal.com_google_guava.guava.v21_0;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import javax.annotation.Nullable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static org.jdbi.v3.lib.internal.com_google_guava.guava.v21_0.Preconditions.checkArgument;

public abstract class TypeParameter<T> extends TypeCapture<T> {
    final TypeVariable<?> typeVariable;

    protected TypeParameter() {
        Type type = capture();
        checkArgument(type instanceof TypeVariable, "%s should be a type variable.", type);
        this.typeVariable = (TypeVariable<?>) type;
    }

    @Override
    public final int hashCode() {
        return typeVariable.hashCode();
    }

    @Override
    @SuppressFBWarnings
    public final boolean equals(@Nullable Object o) {
        if (o instanceof TypeParameter) {
            TypeParameter<?> that = (TypeParameter<?>) o;
            return typeVariable.equals(that.typeVariable);
        }
        return false;
    }

    @Override
    public String toString() {
        return typeVariable.toString();
    }
}
