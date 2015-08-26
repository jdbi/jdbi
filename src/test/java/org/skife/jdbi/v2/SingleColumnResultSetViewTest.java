/*
 * Copyright (C) 2015 Zane Benefits
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

import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(EasyMockRunner.class)
public class SingleColumnResultSetViewTest {

    @Mock
    ResultSet delegate;

    @Mock
    ResultSetMetaData delegateMetaData;

    ResultSet view;

    @Test
    public void shouldExposeSingleColumn() throws Exception {
        mockColumns("foo", "bar", "baz");

        view = SingleColumnResultSetView.newInstance(delegate, 2);
        assertThat(view.getMetaData().getColumnCount(), equalTo(1));
        assertThat(view.findColumn("bar"), equalTo(1));
    }

    @Test(expected = SQLException.class)
    public void shouldThrowOnFindNonExposedColumn() throws Exception {
        mockColumns("foo", "bar", "baz");

        view = SingleColumnResultSetView.newInstance(delegate, 2);

        view.findColumn("foo");
    }

    @Test(expected = SQLException.class)
    public void shouldThrowOnOutOfBoundsColumnIndex() throws Exception {
        mockColumns("foo", "bar", "baz");

        view = SingleColumnResultSetView.newInstance(delegate, 2);

        view.getString(2);
    }

    @Test(expected = SQLException.class)
    public void shouldThrowOnNonExposedColumnLabel() throws Exception {
        mockColumns("foo", "bar", "baz");

        view = SingleColumnResultSetView.newInstance(delegate, 2);

        view.getString("foo");
    }

    @Test
    public void shouldCallDelegate() throws Exception {
        expect(delegate.getString(2)).andReturn("data");
        expect(delegate.getString("bar")).andReturn("data");
        mockColumns("foo", "bar", "baz");

        view = SingleColumnResultSetView.newInstance(delegate, 2);

        assertThat(view.getString(1), equalTo("data"));
        assertThat(view.getString("bar"), equalTo("data"));
    }

    private void mockColumns(String... columnNames) throws Exception {
        expect(delegate.getMetaData()).andReturn(delegateMetaData).anyTimes();
        expect(delegateMetaData.getColumnCount()).andReturn(columnNames.length);
        for (int i = 0; i < columnNames.length; i++) {
            String columnName = columnNames[i];
            int columnNumber = i + 1; // 1-based
            expect(delegate.findColumn(columnName)).andReturn(columnNumber);
            expect(delegateMetaData.getColumnLabel(columnNumber)).andReturn(columnName);
        }

        replay(delegate, delegateMetaData);
    }

}
