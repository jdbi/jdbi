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
package org.skife.jdbi.v2.sqlobject;

import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.unstable.BindIn;

import java.util.ArrayList;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.skife.jdbi.v2.unstable.BindIn.EmptyHandling.NULL;

@RunWith(EasyMockRunner.class)
public class BindInNullMockTest
{
    @Mock
    private BindIn bindInMock;
    @Mock
    private SQLStatement sqlStatementMock;

    // these tests presume a null argument is rendered as the null keyword in the final

    @Test
    public void testOnEmptyNullWithEmptyList() throws Exception
    {
        expect(bindInMock.onEmpty()).andReturn(NULL);
        expect(bindInMock.value()).andReturn("bla");
        replay(bindInMock);

        expect(sqlStatementMock.bind("__bla_0", (String) null)).andReturn(null);
        replay(sqlStatementMock);

        new BindIn.BindingFactory().build(bindInMock).bind(sqlStatementMock, bindInMock, new ArrayList<String>());
    }

    @Test
    public void testOnEmptyNullWithNull() throws Exception
    {
        expect(bindInMock.onEmpty()).andReturn(NULL);
        expect(bindInMock.value()).andReturn("bla");
        replay(bindInMock);

        expect(sqlStatementMock.bind("__bla_0", (String) null)).andReturn(null);
        replay(sqlStatementMock);

        new BindIn.BindingFactory().build(bindInMock).bind(sqlStatementMock, bindInMock, null);
    }
}
