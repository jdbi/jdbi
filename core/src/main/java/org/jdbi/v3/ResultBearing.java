/*
 * Copyright (C) 2004 - 2013 Brian McCallister
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
package org.jdbi.v3;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface ResultBearing<ResultType> extends Iterable<ResultType>
{
    @Override
    public ResultIterator<ResultType> iterator();

    default ResultType first() {
        try (ResultIterator<ResultType> iter = iterator()) {
            return iter.hasNext() ? iter.next() : null;
        }
    }

    default Stream<ResultType> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    default List<ResultType> list() {
        List<ResultType> result = new ArrayList<>();
        for (ResultType item : this) {
            result.add(item);
        }
        return result;
    }
}
