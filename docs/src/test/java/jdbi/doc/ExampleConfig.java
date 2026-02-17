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
package jdbi.doc;

import org.jdbi.core.config.JdbiConfig;

// tag::exampleConfig[]
public class ExampleConfig implements JdbiConfig<ExampleConfig> {

    private String color;
    private int number;

    public ExampleConfig() {
        color = "purple";
        number = 42;
    }

    private ExampleConfig(ExampleConfig other) {
        this.color = other.color;
        this.number = other.number;
    }

    public ExampleConfig setColor(String color) {
        this.color = color;
        return this;
    }

    public String getColor() {
        return color;
    }

    public ExampleConfig setNumber(int number) {
        this.number = number;
        return this;
    }

    public int getNumber() {
        return number;
    }

    @Override
    public ExampleConfig createCopy() {
        return new ExampleConfig(this);
    }

}
// end::exampleConfig[]
