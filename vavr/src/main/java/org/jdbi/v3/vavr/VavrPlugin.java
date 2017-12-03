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
package org.jdbi.v3.vavr;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.spi.JdbiPlugin;

import java.lang.reflect.Type;

/**
 * JDBI-Plugin for vavr.io library
 * <ul>
 *  <li>supports single-value arguments ({@link io.vavr.control.Option}, ...)</li>
 *  <li>supports vavr collections via {@link org.jdbi.v3.core.result.ResultBearing#collectInto(Type)} call</li>
 *  <li>supports key-value mappings of a tuple result (implicitly used by map collectors)</li>
 *  <li>supports tuple projection</li>
 * </ul>
 */
public class VavrPlugin implements JdbiPlugin {

    @Override
    public void customizeJdbi(Jdbi jdbi) {
        jdbi.registerCollector(new VavrCollectorFactory());
        jdbi.registerRowMapper(new VavrTupleRowMapperFactory());
        jdbi.registerArgument(new VavrValueArgumentFactory());
        jdbi.registerColumnMapper(VavrOptionMapper.factory());
    }
}
