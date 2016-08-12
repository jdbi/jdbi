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
package org.skife.jdbi.v2.sqlobject.stringtemplate;

import java.lang.annotation.Annotation;

public class UseStringTemplate3StatementLocatorImpl implements UseStringTemplate3StatementLocator
{
    private static final UseStringTemplate3StatementLocator DEFAULT = createDefault();

    private final String value;
    private final Class errorListener;
    private final boolean cacheable;

    public UseStringTemplate3StatementLocatorImpl(String value, Class errorListener, boolean cacheable)
    {
        assert value != null;
        assert errorListener != null;
        this.value = value;
        this.errorListener = errorListener;
        this.cacheable = cacheable;
    }

    public static UseStringTemplate3StatementLocator defaultInstance()
    {
        return DEFAULT;
    }

    private static UseStringTemplate3StatementLocator createDefault()
    {
        String defaultValue = defaultValueForMethod("value", String.class);
        Class defaultErrorListener = defaultValueForMethod("errorListener", Class.class);
        boolean defaultCacheable = defaultValueForMethod("cacheable", Boolean.class);

        return new UseStringTemplate3StatementLocatorImpl(defaultValue, defaultErrorListener, defaultCacheable);
    }

    private static <T> T defaultValueForMethod(String method, Class<T> returnType)
    {
        try {
            return returnType.cast(UseStringTemplate3StatementLocator.class.getMethod(method).getDefaultValue());
        }
        catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String value()
    {
        return value;
    }

    @Override
    public Class errorListener()
    {
        return errorListener;
    }

    @Override
    public boolean cacheable()
    {
        return cacheable;
    }

    @Override
    public Class<? extends Annotation> annotationType()
    {
        return UseStringTemplate3StatementLocator.class;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UseStringTemplate3StatementLocatorImpl that = (UseStringTemplate3StatementLocatorImpl) o;
        return value.equals(that.value) && errorListener.equals(that.errorListener) && cacheable == that.cacheable;
    }

    @Override
    public int hashCode()
    {
        return ((127 * "value".hashCode()) ^ value().hashCode()) +
            ((127 * "errorListener".hashCode()) ^ errorListener().hashCode()) +
            ((127 * "cacheable".hashCode()) ^ Boolean.valueOf(cacheable).hashCode());
    }
}
