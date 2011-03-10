/*
 * Copyright 2004 - 2011 Brian McCallister
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
import org.skife.jdbi.v2.sqlobject.binders.Binder;

import java.lang.annotation.Annotation;

class Bindifier
{
    private final int    param_idx;
    private final Binder binder;
    private final Annotation annotation;

    Bindifier(Annotation bind, int param_idx, Binder binder)
    {
        this.annotation = bind;
        this.param_idx = param_idx;
        this.binder = binder;
    }

    void bind(SQLStatement q, Object[] args)
    {
        binder.bind(q, annotation, args[param_idx]);
    }
}
