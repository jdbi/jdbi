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



/**
 * Used by {@see AutoConfigurator} to hold found config values
 */
class Configuration
{
    /** jdbc connection string url */
    private String conn;
    private String driver;
    private String user;
    private String pass;
    private String handleDecoratorBuilder;
    private String transactionHandler;
    private String statementLocator;

    String getRowMapper()
    {
        return rowMapper;
    }

    void setRowMapper(String rowMapper)
    {
        this.rowMapper = rowMapper;
    }

    private String rowMapper;

    String getUsername()
    {
        return user;
    }

    void setUsername(String username)
    {
        this.user = username;
    }

    String getPassword()
    {
        return pass;
    }

    void setPassword(String password)
    {
        this.pass = password;
    }

    String getDriver()
    {
        return driver;
    }

    void setDriver(String driver)
    {
        this.driver = driver;
    }

    String getUrl()
    {
        return conn;
    }

    void setUrl(String url)
    {
        this.conn = url;
    }

    void validate()
    {
        if (conn == null) throw new DBIError("must provide a jdbc url property in properties");
        if (driver == null) throw new DBIError("must provide a driver class name in properties");

        if ((user != null && pass == null) || (pass != null && user == null))
        {
            throw new DBIError("If you specify username, or passwoord, in  theproperties, you must specify both");
        }
    }

    public void setHandleDecoratorBuilder(String class_name)
    {
        this.handleDecoratorBuilder = class_name;
    }

    public String getHandleDecoratorBuilder()
    {
        return handleDecoratorBuilder;
    }

    public String getTransactionHandler()
    {
        return transactionHandler;
    }

    public void setTransactionHandler(String transactionHandler)
    {
        this.transactionHandler = transactionHandler;
    }

    public void setStatementLocator(String property)
    {
        this.statementLocator = property;
    }

    public String getStatementLocator()
    {
        return statementLocator;
    }
}
