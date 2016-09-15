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
package org.jdbi.v3.sqlobject;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

import org.jdbi.v3.core.SqlStatement;
import org.jdbi.v3.sqlobject.unstable.BindIn;
import org.jdbi.v3.sqlobject.unstable.BindIn.EmptyHandling;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BindInNullMockTest
{
    @Mock
    private BindIn bindInMock;
    @Mock
    private SqlStatement<?> sqlStatementMock;

    // these tests presume a null argument is rendered as the null keyword in the final

    @Test
    public void testOnEmptyNullWithEmptyList() throws Exception
    {
        when(bindInMock.onEmpty()).thenReturn(EmptyHandling.NULL);
        when(bindInMock.value()).thenReturn("bla");

        new BindIn.BindingFactory().build(bindInMock).bind(sqlStatementMock, null, 0, bindInMock, new ArrayList<String>());

        verify(sqlStatementMock).bind("__bla_0", (String) null);
    }

    @Test
    public void testOnEmptyNullWithNull() throws Exception
    {
        when(bindInMock.onEmpty()).thenReturn(EmptyHandling.NULL);
        when(bindInMock.value()).thenReturn("bla");

        new BindIn.BindingFactory().build(bindInMock).bind(sqlStatementMock, null, 0, bindInMock, null);

        verify(sqlStatementMock).bind("__bla_0", (String) null);
    }
}
