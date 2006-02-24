/* Copyright 2004-2006 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.skife.jdbi;

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;
import java.util.Map;

import org.skife.jdbi.unstable.decorator.BaseHandleDecorator;
import org.skife.jdbi.unstable.decorator.HandleDecorator;
import org.skife.jdbi.derby.Tools;

public class TestAutoConfigurator extends TestCase
{
    private ClassLoader original;
    private MyClassLoader loader;
    private AutoConfigurator auto;

    public void setUp() throws Exception
    {
        loader = new MyClassLoader();
        original = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(loader);
        auto = new AutoConfigurator();
    }

    public void tearDown() throws Exception
    {
        Thread.currentThread().setContextClassLoader(original);
    }

    public void testJDBC() throws Exception
    {
        final Properties jdbc = new Properties();
        jdbc.setProperty("jdbc.driver", "DRIVER");
        jdbc.setProperty("jdbc.url", "URL");

        loader.setProperties("jdbc.properties", jdbc);
        Configuration config = auto.configure();
        assertEquals("URL", config.getUrl());
        assertEquals("DRIVER", config.getDriver());
        assertNull(config.getUsername());
        assertNull(config.getPassword());
    }

    public void testPropNameCombos() throws Exception
    {
        final Properties jdbc = new Properties();
        jdbc.setProperty("jdbc.drive", "DRIVER");
        jdbc.setProperty("jdbc.url", "URL");

        loader.setProperties("jdbc.properties", jdbc);
        Configuration config = auto.configure();
        assertEquals("URL", config.getUrl());
        assertEquals("DRIVER", config.getDriver());
        assertNull(config.getUsername());
        assertNull(config.getPassword());
    }

    public void testJDBIAndJDBC() throws Exception
    {
        final Properties jdbc = new Properties();
        jdbc.setProperty("jdbc.driver", "WRONG");
        jdbc.setProperty("jdbc.url", "WRONG");

        final Properties jdbi = new Properties();
        jdbi.setProperty("jdbc.driver", "DRIVER");
        jdbi.setProperty("jdbc.url", "URL");

        loader.setProperties("jdbc.properties", jdbc);
        loader.setProperties("jdbi.properties", jdbi);

        Configuration config = auto.configure();
        assertEquals("URL", config.getUrl());
        assertEquals("DRIVER", config.getDriver());
        assertNull(config.getUsername());
        assertNull(config.getPassword());
    }

    private class MyClassLoader extends ClassLoader
    {
        private HashMap cache = new HashMap();

        public void setProperties(final String name, final Properties props) throws IOException
        {
            final ByteArrayOutputStream outs = new ByteArrayOutputStream();
            props.store(outs, "test data");
            cache.put(name, outs.toByteArray());
        }

        public InputStream getResourceAsStream(String s)
        {
            if (cache.containsKey(s))
            {
                final byte[] bytes = (byte[]) cache.get(s);
                final ByteArrayInputStream ins = new ByteArrayInputStream(bytes);
                return ins;
            }
            else
            {
                return super.getResourceAsStream(s);
            }
        }
    }
}
