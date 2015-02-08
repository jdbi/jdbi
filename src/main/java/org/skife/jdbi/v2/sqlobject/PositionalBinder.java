/*
 * Copyright (C) 2004 - 2014 Brian McCallister
 *
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
package org.skife.jdbi.v2.sqlobject;

import org.skife.jdbi.v2.SQLStatement;

import java.lang.annotation.Annotation;

/**
 * A binder that bind an argument by its position in the arguments list
 */
public class PositionalBinder implements Binder<Annotation, Object> {

    private final int position;

    public PositionalBinder(int position) {
        this.position = position;
    }

    @Override
    public void bind(SQLStatement<?> q, Annotation bind, Object arg) {
        q.bind(String.valueOf(position), arg);
    }
}
