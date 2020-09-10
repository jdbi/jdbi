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
package org.jdbi.v3.benchmark.sqlobject;

import java.util.StringJoiner;

public class TestData {

    private final long id;

    private final TestContent content;

    public TestData(final long id, final TestContent content) {
        this.id = id;
        this.content = content;
    }

    public long getId() {
        return id;
    }

    public TestContent getContent() {
        return content;
    }

    public String getName() {
        return content.getName();
    }

    public String getDescription() {
        return content.getDescription();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final TestData testData = (TestData) o;

        if (id != testData.id) {
            return false;
        }
        return content != null ? content.equals(testData.content) : testData.content == null;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (content != null ? content.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", TestData.class.getSimpleName() + "[", "]")
                .add("id=" + id)
                .add("content=" + content)
                .toString();
    }
}
