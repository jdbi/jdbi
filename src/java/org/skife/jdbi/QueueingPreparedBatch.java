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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class QueueingPreparedBatch implements PreparedBatch
{
    private final PreparedStatement stmt;
    private final String[] params;
    private List args = new ArrayList();

    QueueingPreparedBatch(PreparedStatement stmt, String[] params)
    {
        this.stmt = stmt;
        this.params = params;
    }

    public PreparedBatch add(Object[] objects)
    {
        args.add(new ArrayHolder(objects));
        return this;
    }

    public PreparedBatch add(Collection params)
    {
        args.add(new CollectionHolder(params));
        return this;
    }

    public PreparedBatch add(Object bean)
    {
        args.add(new BeanHolder(bean));
        return this;
    }

    public PreparedBatch addAll(Collection args)
    {
        for (Iterator iterator = args.iterator(); iterator.hasNext();)
        {
            final Object o = iterator.next();
            if (o instanceof Map)
            {
                add((Map) o);
            }
            else if (o instanceof Collection)
            {
                add((Collection) o);
            }
            else if (o instanceof Object[])
            {
                add((Object[]) o);
            }
            else
            {
                add(o);
            }
        }
        return this;
    }

    public PreparedBatch addAll(Object[] args)
    {
        for (int i = 0; i < args.length; i++)
        {
            final Object o = args[i];
            if (o instanceof Map)
            {
                add((Map) o);
            }
            else if (o instanceof Collection)
            {
                add((Collection) o);
            }
            else if (o instanceof Object[])
            {
                add((Object[]) o);
            }
            else
            {
                add(o);
            }
        }
        return this;
    }

    public PreparedBatch add(Map params)
    {
        args.add(new MapHolder(params));
        return this;
    }

    public int[] execute() throws DBIException
    {
        try
        {
            for (int i = 0; i < args.size(); i++)
            {
                final Arguments h = (Arguments) args.get(i);
                final Object[] objects = h.objects();
                for (int j = 0; j < objects.length; j++)
                {
                    final Object object = objects[j];
                    stmt.setObject(j + 1, object);

                }
                stmt.addBatch();
            }
            final int[] results = stmt.executeBatch();
            return results;
        }
        catch (SQLException e)
        {
            throw new DBIException(e.getMessage(), e);
        }
        finally
        {
            args.clear();
        }
    }

    private class ArrayHolder implements Arguments
    {
        private final Object[] args;

        ArrayHolder(Object[] args)
        {
            this.args = args;
        }

        public Object[] objects()
        {
            return args;
        }
    }

    private class BeanHolder implements Arguments
    {
        private final Object[] args;

        public BeanHolder(Object bean)
        {
            args = ParamTool.getParamsForBean(params, bean);
        }

        public Object[] objects()
        {
            return args;
        }
    }

    private class MapHolder implements Arguments
    {
        final Object[] args;

        MapHolder(Map map)
        {
            args = ParamTool.getParamsFromMap(params, map);
        }

        public Object[] objects()
        {
            return args;
        }
    }

    private class CollectionHolder implements Arguments
    {
        final Object[] args;

        public CollectionHolder(Collection params)
        {
            args = ParamTool.getParamsFromCollection(params);
        }

        public Object[] objects()
        {
            return args;
        }
    }
}
