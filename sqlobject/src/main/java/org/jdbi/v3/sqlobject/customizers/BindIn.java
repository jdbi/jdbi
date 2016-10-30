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
package org.jdbi.v3.sqlobject.customizers;

import static org.jdbi.v3.core.util.GenericTypes.findGenericParameter;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.jdbi.v3.sqlobject.Binder;
import org.jdbi.v3.sqlobject.BinderFactory;
import org.jdbi.v3.sqlobject.BindingAnnotation;
import org.jdbi.v3.sqlobject.SqlStatementCustomizer;
import org.jdbi.v3.sqlobject.SqlStatementCustomizerFactory;
import org.jdbi.v3.sqlobject.SqlStatementCustomizingAnnotation;
import org.jdbi.v3.sqlobject.internal.ParameterUtil;

/**
 * Binds each value in the annotated {@link Iterable} or array/varargs argument, and defines a named attribute as a
 * comma-separated list of each bound parameter name. Common use cases:
 *
 * <pre>
 * &#64;SqlQuery("select * from things where id in (&lt;ids&gt;)")
 * List&lt;Thing&gt; getThings(@BindIn int... ids)
 *
 * &#64;SqlQuery("insert into things (&lt;columnNames&gt;) values (&lt;values&gt;)")
 * void insertThings(@DefineIn List&lt;String&gt; columnNames, @BindIn List&lt;Object&gt; values)
 * </pre>
 *
 * <p>
 * Throws IllegalArgumentException if the argument is not an array or Iterable. How null and empty collections are handled can be configured with onEmpty:EmptyHandling - throws IllegalArgumentException by default.
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
@SqlStatementCustomizingAnnotation(BindIn.CustomizerFactory.class)
@BindingAnnotation(BindIn.BindingFactory.class)
public @interface BindIn
{
    /**
     * The attribute name to define. If omitted, the name of the annotated parameter is used. It is an error to omit
     * the name when there is no parameter naming information in your class files.
     *
     * @return the attribute name.
     */
    String value() default "";

    /**
     * what to do when the argument is null or empty
     */
    EmptyHandling onEmpty() default EmptyHandling.THROW;

    final class CustomizerFactory implements SqlStatementCustomizerFactory
    {

        @Override
        public SqlStatementCustomizer createForParameter(final Annotation annotation, final Class<?> sqlObjectType, final Method method, Parameter param, final Object arg)
        {
            final BindIn bindIn = (BindIn) annotation;

            final int size;
            if (arg == null)
            {
                switch (bindIn.onEmpty())
                {
                    case VOID:
                        // skip argument iteration altogether to output nothing at all
                        size = 0;
                        break;
                    case NULL:
                        // output exactly 1 value: null
                        size = 1;
                        break;
                    case THROW:
                        throw new IllegalArgumentException("argument is null; null was explicitly forbidden on this instance of BindIn");
                    default:
                        throw new IllegalStateException(EmptyHandling.valueNotHandledMessage);
                }
            } else
            {
                size = Util.size(arg);
            }

            if (size == 0)
            {
                switch (bindIn.onEmpty())
                {
                    case VOID:
                        // output nothing - taken care of with size = 0
                        break;
                    case NULL:
                        // output null - handle in Binder
                        break;
                    case THROW:
                        throw new IllegalArgumentException("argument is empty; emptiness was explicitly forbidden on this instance of BindIn");
                    default:
                        throw new IllegalStateException(EmptyHandling.valueNotHandledMessage);
                }
            }

            final String name = ParameterUtil.getParameterName(bindIn, bindIn.value(), param);

            // generate and concat placeholders
            final StringBuilder names = new StringBuilder();
            for (int i = 0; i < size; i++)
            {
                if (i > 0)
                {
                    names.append(",");
                }
                names.append(":__").append(name).append("_").append(i);
            }
            final String ns = names.toString();

            return q -> q.define(name, ns);
        }
    }

    class BindingFactory implements BinderFactory<BindIn, Object>
    {
        @Override
        public Binder<BindIn, Object> build(final BindIn bindIn)
        {
            return (q, param, index, bind, arg) -> {
                final String name = ParameterUtil.getParameterName(bindIn, bindIn.value(), param);

                if (arg == null || Util.size(arg) == 0)
                {
                    switch (bindIn.onEmpty())
                    {
                        case VOID:
                            // output nothing, end now
                            break;
                        case NULL:
                            // output null
                            q.bind("__" + name + "_0", (String) null);
                            break;
                        case THROW:
                            final Exception inner = new IllegalArgumentException("argument is null; null was explicitly forbidden on this instance of BindIn");
                            throw new IllegalStateException("Illegal argument value was caught too late. Please report this to the jdbi developers.", inner);
                        default:
                            throw new IllegalStateException(EmptyHandling.valueNotHandledMessage);
                    }
                } else
                {
                    Type parameterType = param == null ? Object.class : param.getParameterizedType();
                    Type elementType = findGenericParameter(parameterType, Iterable.class)
                            .orElse(Object.class);
                    // replace placeholders with actual values
                    final Iterator<?> it = Util.toIterator(arg);
                    for (int i = 0; it.hasNext(); i++)
                    {
                        q.bindByType("__" + name + "_" + i, it.next(), elementType);
                    }
                }
            };
        }
    }

    final class Util
    {
        private Util()
        {
        }

        static Iterator<?> toIterator(final Object obj)
        {
            if (obj == null)
            {
                throw new IllegalArgumentException("cannot make iterator of null");
            }

            if (obj instanceof Iterable)
            {
                return ((Iterable<?>) obj).iterator();
            }

            if (obj.getClass().isArray())
            {
                if (obj instanceof Object[])
                {
                    return Arrays.asList((Object[]) obj).iterator();
                } else
                {
                    return new ReflectionArrayIterator(obj);
                }
            }

            throw new IllegalArgumentException(getTypeWarning(obj.getClass()));
        }

        static int size(final Object obj)
        {
            if (obj == null)
            {
                throw new IllegalArgumentException("cannot get size of null");
            }

            if (obj instanceof Collection)
            {
                return ((Collection<?>) obj).size();
            }

            if (obj instanceof Iterable)
            {
                final Iterable<?> iterable = (Iterable<?>) obj;

                int size = 0;
                for (final Object x : iterable)
                {
                    size++;
                }

                return size;
            }

            if (obj.getClass().isArray())
            {
                return Array.getLength(obj);
            }

            throw new IllegalArgumentException(getTypeWarning(obj.getClass()));
        }

        private static String getTypeWarning(final Class<?> type)
        {
            return "argument must be one of the following: Iterable, or an array/varargs (primitive or complex type); was " + type.getName() + " instead";
        }
    }

    /**
     * describes what needs to be done if the passed argument is null or empty
     */
    enum EmptyHandling
    {
        /**
         * output "" (without quotes, i.e. nothing)
         *
         * select * from things where x in ()
         */
        VOID,
        /**
         * output "null" (without quotes, as keyword), useful e.g. in postgresql where "in ()" is invalid syntax
         *
         * select * from things where x in (null)
         */
        NULL,
        /**
         * throw IllegalArgumentException
         */
        THROW;

        static final String valueNotHandledMessage = "EmptyHandling type on BindIn not handled. Please report this to the jdbi developers.";
    }
}
