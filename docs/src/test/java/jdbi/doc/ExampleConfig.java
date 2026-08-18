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

    // A JdbiConfig is immutable: its fields are final, and a "wither" returns a new instance instead of mutating.
    private final String color;
    private final int number;

    // Public no-argument constructor providing the defaults.
    public ExampleConfig() {
        this("purple", 42);
    }

    // Private all-arguments constructor used by the withers.
    private ExampleConfig(String color, int number) {
        this.color = color;
        this.number = number;
    }

    public ExampleConfig color(String color) {
        return new ExampleConfig(color, this.number);
    }

    public String getColor() {
        return color;
    }

    public ExampleConfig number(int number) {
        return new ExampleConfig(this.color, number);
    }

    public int getNumber() {
        return number;
    }
}
// end::exampleConfig[]
