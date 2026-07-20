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
package org.jdbi.core.statement;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import org.jdbi.core.Handle;
import org.jdbi.core.internal.testing.H2DatabaseExtension;
import org.jdbi.core.statement.internal.JfrSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static java.util.stream.Collectors.toList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

public class TestStatementJfr {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.withSomething();

    /**
     * A query built from a reusable template runs through the same execution engine as a classic query, so it
     * is instrumented with a {@link JdbiStatementEvent} like any other statement. (Before the engine was
     * unified, template execution emitted no event.)
     */
    @Test
    public void templateQueryEmitsStatementEvent() throws Exception {
        assumeThat(JfrSupport.isFlightRecorderAvailable()).isTrue();

        final Handle h = h2Extension.getSharedHandle();
        h.execute("insert into something (id, name) values (1, 'eric')");

        final Path recordingFile = Files.createTempFile("jdbi-statement-", ".jfr");
        try (Recording recording = new Recording()) {
            recording.enable(JdbiStatementEvent.NAME);
            recording.start();

            final var template = h.getJdbi().buildStatementTemplate("select name from something where id = :id");
            final String name = template.with(h).bind("id", 1).mapTo(String.class).one();
            assertThat(name).isEqualTo("eric");

            recording.stop();
            recording.dump(recordingFile);

            final List<RecordedEvent> events = RecordingFile.readAllEvents(recordingFile).stream()
                .filter(e -> JdbiStatementEvent.NAME.equals(e.getEventType().getName()))
                .collect(toList());

            assertThat(events).anySatisfy(event -> {
                assertThat(event.getString("type")).isEqualTo("Query");
                assertThat(event.getString("sql")).contains("select name from something where id");
                assertThat(event.getLong("rowsMapped")).isEqualTo(1);
            });
        } finally {
            Files.deleteIfExists(recordingFile);
        }
    }
}
