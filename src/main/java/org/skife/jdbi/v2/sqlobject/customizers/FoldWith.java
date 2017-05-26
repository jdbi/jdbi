package org.skife.jdbi.v2.sqlobject.customizers;

import org.skife.jdbi.v2.*;
import org.skife.jdbi.v2.sqlobject.SqlStatementCustomizer;
import org.skife.jdbi.v2.sqlobject.SqlStatementCustomizerFactory;
import org.skife.jdbi.v2.sqlobject.SqlStatementCustomizingAnnotation;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Used to register a TypedFolder2 with either a sql object type or for a specific method.
 *
 * The initial value for the accumulator will be an instance of the Class returned by {@link org.skife.jdbi.v2.TypedFolder2#getAccumulatorType()} or null if it can't be instantiated.
 */
@SqlStatementCustomizingAnnotation(FoldWith.Factory.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface FoldWith
{
    /**
     * The result set TypedFolder2 class to register
     */
    Class<? extends TypedFolder2<?>>[] value();

    static class Factory implements SqlStatementCustomizerFactory
    {
        public SqlStatementCustomizer createForMethod(Annotation annotation, Class sqlObjectType, Method method)
        {
            if (annotation instanceof FoldWith) {
                return createASqlStatementCustomizer((FoldWith) annotation);
            } else {
                throw new IllegalStateException("Annotation wasn't an instance of FoldWith");
            }
        }

        public SqlStatementCustomizer createForType(Annotation annotation, Class sqlObjectType)
        {
            if (annotation instanceof FoldWith) {
                return createASqlStatementCustomizer((FoldWith) annotation);
            } else {
                throw new IllegalStateException("Annotation wasn't an instance of FoldWith");
            }
        }

        public SqlStatementCustomizer createForParameter(Annotation annotation, Class sqlObjectType, Method method, Object arg)
        {
            throw new UnsupportedOperationException("Not defined for parameter");
        }

        private static SqlStatementCustomizer createASqlStatementCustomizer(FoldWith foldWithAnnotation) {
            final TypedFolder2[] typedFolders = new TypedFolder2[foldWithAnnotation.value().length];
            Class<? extends TypedFolder2<?>>[] folderClasses = foldWithAnnotation.value();
            for (int i = 0; i < folderClasses.length; i++) {
                Class<? extends TypedFolder2<?>> clazz =  folderClasses[i];
                try
                {
                    typedFolders[i] = clazz.newInstance();
                }
                catch (Exception e)
                {
                    throw new IllegalStateException("Unable to create an instance of " + clazz.getClass().getCanonicalName(), e);
                }
            }
            return new SqlStatementCustomizer()
            {
                public void apply(SQLStatement statement)
                {
                    if (statement instanceof Query) {
                        final Query q = (Query) statement;
                        for (TypedFolder2 folder : typedFolders) {
                            final Class accumulatorType = folder.getAccumulatorType();
                            Object defaultAccumulator = null;
                            try {
                                defaultAccumulator = accumulatorType.newInstance();
                            }
                            catch (InstantiationException ie)
                            {}
                            catch (IllegalAccessException iae)
                            {}
                            final Object foldResult = q.fold(defaultAccumulator, folder);
                            q.registerMapper(new ResultSetMapperFactory() {
                                @Override
                                public boolean accepts(Class type, StatementContext ctx) {
                                    return accumulatorType.equals(type);
                                }

                                @Override
                                public ResultSetMapper mapperFor(Class type, StatementContext ctx) {
                                    return new ResultSetMapper() {
                                        @Override
                                        public Object map(int index, ResultSet r, StatementContext ctx) throws SQLException {
                                            return foldResult;
                                        }
                                    };
                                }
                            });
                        }
                    }
                }
            };
        }
    }
}
