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
package org.skife.jdbi.v2;

import org.easymock.EasyMock;
import org.junit.Test;
import org.skife.jdbi.v2.tweak.Argument;

import java.sql.PreparedStatement;

public class TestBooleanIntegerArgument {

    @Test
    public void testTrue() throws Exception {

        PreparedStatement mockStmt = EasyMock.createMock(PreparedStatement.class);
        mockStmt.setInt(5, 1);

        EasyMock.replay(mockStmt);

        Argument arrrgh = new BooleanIntegerArgument(true);

        arrrgh.apply(5, mockStmt, null);

        EasyMock.verify(mockStmt);
    }

    @Test
    public void testFalse() throws Exception {

        PreparedStatement mockStmt = EasyMock.createMock(PreparedStatement.class);
        mockStmt.setInt(5, 0);

        EasyMock.replay(mockStmt);

        Argument arrrgh = new BooleanIntegerArgument(false);

        arrrgh.apply(5, mockStmt, null);

        EasyMock.verify(mockStmt);
    }
}
