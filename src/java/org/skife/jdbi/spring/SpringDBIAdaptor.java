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
package org.skife.jdbi.spring;

import org.skife.jdbi.IDBI;
import org.skife.jdbi.Handle;
import org.skife.jdbi.DBIException;
import org.skife.jdbi.HandleCallback;

import java.util.Map;
import java.io.IOException;

class SpringDBIAdaptor implements IDBI
{
    private final IDBI real;

    SpringDBIAdaptor(IDBI real)
    {
        this.real = real;
    }

    public Handle open() throws DBIException
    {
        return new SpringHandleAdaptor(real.open(), this);
    }

    public void open(HandleCallback callback) throws DBIException
    {
        final Handle bound = DBIUtils.getHandle(this);
        try
        {
            callback.withHandle(bound);
        }
        catch (Exception e)
        {
            throw new DBIException(e.getMessage(), e);
        }
        finally
        {
            DBIUtils.closeHandleIfNecessary(bound, this);
        }
    }

    public Map getNamedStatements()
    {
        return real.getNamedStatements();
    }

    public void name(String name, String statement) throws DBIException
    {
        real.name(name, statement);
    }

    public void load(String name) throws DBIException, IOException
    {
        real.load(name);
    }
}
