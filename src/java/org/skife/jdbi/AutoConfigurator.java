/* Copyright 2004-2005 Brian McCallister
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

import org.skife.jdbi.unstable.decorator.HandleDecorator;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Searches for appropriate source of configuration information, and remembers it!
 */
class AutoConfigurator
{
    private static final String SYSTEM_PROPERTY_NAME_OVERRIDE = "org.skife.jdbi.properties-file";

    private static final String[] PROPERTIES_FILE_NAMES = new String[]
    {
        (System.getProperty(SYSTEM_PROPERTY_NAME_OVERRIDE) != null
         ? System.getProperty(SYSTEM_PROPERTY_NAME_OVERRIDE)
         : "No Such Property Name, Thank You Very Much, Please Try Another"),
        "jdbi.properties", "jdbc.properties", "dbi.properties", "database.properties"
    };

    private static final String[] JDBC_URL_PROPS = new String[]
    {
        "jdbi.url", "jdbc.url", "connection.string"
    };

    private static final String[] DRIVER_NAME_PROPS = new String[]
    {
        "jdbi.driver", "jdbc.driver", "driver", "jdbc.drive"
    };

    private static final String[] USERNAME_PROPS = new String[]
    {
        "jdbi.username", "jdbi.user", "jdbc.username", "jdbc.user", "username", "user"
    };

    private static final String[] PASSWORD_PROPS = new String[]
    {
        "jdbi.password", "jdbi.pass", "jdbc.password", "jdbc.pass", "pass", "password"
    };

    private static final String[] HANDLE_DECORATOR_BUIDLER = new String[]
    {
        "jdbi.handle-decorator-builder",
        "jdbc.handle-decorator-builder",
        "handle-decorator-builder"
    };

    private static ConnectionFactory factory = null;
    private static String handleDecoratorBuilder = null;

    /**
     * will return the cached connection factory, or try to build a new one
     *
     * @throws IOException on errors while loading properties
     */
    ConnectionFactory getConnectionFactory() throws IOException
    {
        if (factory != null) return factory;

        // not already self-configured so go to town

        final Configuration config = configure();
        config.validate();
        final String conn = config.getUrl();
        final String driver = config.getDriver();
        final String user = config.getUsername();
        final String pass = config.getPassword();
        try
        {
            Class.forName(driver);
        }
        catch (ClassNotFoundException e)
        {
            throw new DBIError("The JDBC Driver class name provided [" + driver + "] cannot be found.");
        }
        if (user == null)
        {
            factory = new ConnectionFactory()
            {
                public Connection getConnection() throws SQLException
                {
                    return DriverManager.getConnection(conn);
                }
            };
        }
        else
        {
            factory = new ConnectionFactory()
            {
                public Connection getConnection() throws SQLException
                {
                    return DriverManager.getConnection(conn, user, pass);
                }
            };
        }

        handleDecoratorBuilder = config.getHandleDecoratorBuilder();

        return factory;
    }

    Configuration configure() throws IOException
    {
        final Properties props = mangleProperties(findProperties());
        final Configuration config = new Configuration();
        config.setDriver(props.getProperty(DRIVER_NAME_PROPS[0]));
        config.setPassword(props.getProperty(PASSWORD_PROPS[0]));
        config.setUrl(props.getProperty(JDBC_URL_PROPS[0]));
        config.setUsername(props.getProperty(USERNAME_PROPS[0]));
        config.setHandleDecoratorBuilder(props.getProperty(HANDLE_DECORATOR_BUIDLER[0]));
        return config;
    }

    private Properties findProperties() throws IOException
    {
        final Properties props = new Properties();

        for (int i = 0; i < PROPERTIES_FILE_NAMES.length; i++)
        {
            final String name = PROPERTIES_FILE_NAMES[i];
            final InputStream in = StatementCache.selectClassLoader().getResourceAsStream(name);
            if (in != null)
            {
                props.load(in);
                in.close();
                return props;
            }
        }

        throw new IllegalStateException("unable to findInternal configuration properties on classpath");
    }

    private Properties mangleProperties(Properties starting)
    {
        final String driver = selectFirst(DRIVER_NAME_PROPS, starting);
        if (driver != null) starting.setProperty(DRIVER_NAME_PROPS[0], driver);
        final String jdbc_url = selectFirst(JDBC_URL_PROPS, starting);
        if (jdbc_url != null) starting.setProperty(JDBC_URL_PROPS[0], jdbc_url);
        final String username = selectFirst(USERNAME_PROPS, starting);
        if (username != null) starting.setProperty(USERNAME_PROPS[0], username);
        final String password = selectFirst(PASSWORD_PROPS, starting);
        if (password != null) starting.setProperty(PASSWORD_PROPS[0], password);
        final String decorator = selectFirst(HANDLE_DECORATOR_BUIDLER, starting);
        if (decorator != null) starting.setProperty(HANDLE_DECORATOR_BUIDLER[0], decorator);
        return starting;
    }

    private String selectFirst(String[] options, Properties props)
    {
        if (System.getProperty(options[0]) != null) return System.getProperty(options[0]);
        for (int i = 0; i < options.length; i++)
        {
            final String option = options[i];
            if (props.containsKey(option)) return props.getProperty(option);
        }
        return null;
    }

    public HandleDecorator getHandleDecoratorBuilder()
            throws ClassNotFoundException, IllegalAccessException, InstantiationException
    {
        if (handleDecoratorBuilder == null) return null;
        Class clazz = Class.forName(handleDecoratorBuilder);
        HandleDecorator builder = (HandleDecorator) clazz.newInstance();
        return builder;
    }
}
