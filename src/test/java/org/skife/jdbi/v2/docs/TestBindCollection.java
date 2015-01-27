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
package org.skife.jdbi.v2.docs;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.logging.PrintStreamLog;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.stringtemplate.UseStringTemplate3StatementLocator;
import org.skife.jdbi.v2.sqlobject.BindCollection;

import java.util.*;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class TestBindCollection
{
    private DBI    dbi;
    private Handle handle;

    @Before
    public void setUp() throws Exception
    {
        dbi = new DBI("jdbc:h2:mem:" + UUID.randomUUID());

        dbi.setSQLLog(new PrintStreamLog(System.out));
        handle = dbi.open();
        handle.execute("create table keyvalues (id integer primary key, key varchar(100), value varchar(100))");
    }

    @After
    public void tearDown() throws Exception
    {
        handle.close();
    }

    @Test
    public void testCollectionValueBinding() throws Exception
    {
        handle.execute("insert into keyvalues (id, key, value) values (1, 'k1', 'v1'), (2, 'k1', 'blah'), (3, 'k2', 'v2')");

        DAO dao = handle.attach(DAO.class);

        Collection<String> values = asList("%1%", "%2");

        List<Integer> results = dao.findValuesLikeAnyOf(values);

        assertThat(results, equalTo(asList(1, 3)));
    }

    @Test
    public void testCollectionBeanBinding() throws Exception
    {
        handle.execute("insert into keyvalues (id, key, value) values (1, 'k1', 'v1'), (2, 'k1', 'blah'), (3, 'k2', 'v2')");

        DAO dao = handle.attach(DAO.class);

        Collection<KeyValue> keyValuePairs = asList(new KeyValue("k1", "v1"), new KeyValue("k2", "v2"));

        List<Integer> results = dao.findAnyWithAKeyAndValue(keyValuePairs);

        assertThat(results, equalTo(asList(1, 3)));
    }

    @UseStringTemplate3StatementLocator
    public static interface DAO
    {
        @SqlQuery
        public List<Integer> findAnyWithAKeyAndValue(@BindCollection("keyValues") Collection<KeyValue> keyValues);


        @SqlQuery
        public List<Integer> findValuesLikeAnyOf(@BindCollection("values") Collection<String> values);
    }

    public static class KeyValue {
        private String key;
        private String value;

        public KeyValue(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
