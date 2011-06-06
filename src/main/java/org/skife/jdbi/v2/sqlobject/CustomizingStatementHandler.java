package org.skife.jdbi.v2.sqlobject;

import com.fasterxml.classmate.members.ResolvedMethod;
import org.skife.jdbi.v2.ConcreteStatementContext;
import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.exceptions.UnableToCreateStatementException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

abstract class CustomizingStatementHandler implements Handler
{
    private final List<Bindifier>                    binders                        = new ArrayList<Bindifier>();
    private final List<FactoryAnnotationPair>        typeBasedCustomizerFactories   = new ArrayList<FactoryAnnotationPair>();
    private final List<FactoryAnnotationPair>        methodBasedCustomizerFactories = new ArrayList<FactoryAnnotationPair>();
    private final List<FactoryAnnotationIndexTriple> paramBasedCustomizerFactories  = new ArrayList<FactoryAnnotationIndexTriple>();
    private final Class  sqlObjectType;
    private final Method method;

    CustomizingStatementHandler(Class sqlObjectType, ResolvedMethod method)
    {
        this.sqlObjectType = sqlObjectType;
        this.method = method.getRawMember();

        for (final Annotation annotation : sqlObjectType.getAnnotations()) {
            if (annotation.annotationType().isAnnotationPresent(SqlStatementCustomizingAnnotation.class)) {
                final SqlStatementCustomizingAnnotation a = annotation.annotationType()
                                                                      .getAnnotation(SqlStatementCustomizingAnnotation.class);
                final SqlStatementCustomizerFactory f;
                try {
                    f = a.value().newInstance();
                }
                catch (Exception e) {
                    throw new IllegalStateException("unable to create sql statement customizer factory", e);
                }
                typeBasedCustomizerFactories.add(new FactoryAnnotationPair(f, annotation));
            }
        }


        final Annotation[] method_annotations = method.getRawMember().getAnnotations();
        for (final Annotation method_annotation : method_annotations) {
            final Class<? extends Annotation> m_anno_class = method_annotation.annotationType();
            if (m_anno_class.isAnnotationPresent(SqlStatementCustomizingAnnotation.class)) {
                final SqlStatementCustomizingAnnotation scf =
                    m_anno_class.getAnnotation(SqlStatementCustomizingAnnotation.class);
                final SqlStatementCustomizerFactory f;
                try {
                    f = scf.value().newInstance();
                }
                catch (Exception e) {
                    throw new IllegalStateException("unable to instantiate statement customizer factory", e);
                }
                methodBasedCustomizerFactories.add(new FactoryAnnotationPair(f, method_annotation));
            }

        }

        final Annotation[][] param_annotations = method.getRawMember().getParameterAnnotations();
        for (int param_idx = 0; param_idx < param_annotations.length; param_idx++) {
            final Annotation[] annotations = param_annotations[param_idx];
            for (final Annotation annotation : annotations) {
                final Class<? extends Annotation> anno_class = annotation.annotationType();


                if (anno_class.isAnnotationPresent(BindingAnnotation.class)) {
                    // we have a binder
                    BindingAnnotation ba = annotation.annotationType().getAnnotation(BindingAnnotation.class);
                    try {
                        BinderFactory fact = ba.value().newInstance();
                        binders.add(new Bindifier(annotation, param_idx, fact.build(annotation)));

                    }
                    catch (Exception e) {
                        throw new IllegalStateException("unable to instantiate cusotmizer", e);
                    }
                }

                if (anno_class.isAnnotationPresent(SqlStatementCustomizingAnnotation.class)) {
                    SqlStatementCustomizingAnnotation sca = annotation.annotationType()
                                                                      .getAnnotation(SqlStatementCustomizingAnnotation.class);
                    final SqlStatementCustomizerFactory f;
                    try {
                        f = sca.value().newInstance();
                    }
                    catch (Exception e) {
                        throw new IllegalStateException("unable to instantiate sql statement customizer factory", e);
                    }
                    paramBasedCustomizerFactories.add(new FactoryAnnotationIndexTriple(f, annotation, param_idx));

                }
            }
        }
    }

    protected final void populateSqlObjectData(ConcreteStatementContext q)
    {
        q.setSqlObjectMethod(method);
        q.setSqlObjectType(sqlObjectType);
    }

    protected void applyBinders(SQLStatement q, Object[] args)
    {
        for (Bindifier binder : binders) {
            binder.bind(q, args);
        }
    }

    protected void applyCustomizers(SQLStatement q, Object[] args)
    {
        for (FactoryAnnotationPair pair : typeBasedCustomizerFactories) {
            try {
                pair.factory.createForType(pair.annotation, sqlObjectType).apply(q);
            }
            catch (SQLException e) {
                throw new UnableToCreateStatementException("unable to apply customizer", e, q.getContext());
            }
        }

        for (FactoryAnnotationPair pair : methodBasedCustomizerFactories) {
            try {
                pair.factory.createForMethod(pair.annotation, sqlObjectType, method).apply(q);
            }
            catch (SQLException e) {
                throw new UnableToCreateStatementException("unable to apply customizer", e, q.getContext());
            }
        }

        if (args != null) {
            for (FactoryAnnotationIndexTriple triple : paramBasedCustomizerFactories) {
                try {
                    triple.factory
                        .createForParameter(triple.annotation, sqlObjectType, method, args[triple.index])
                        .apply(q);
                }
                catch (SQLException e) {
                    throw new UnableToCreateStatementException("unable to apply customizer", e, q.getContext());
                }
            }
        }
    }

    private static class FactoryAnnotationPair
    {
        private final SqlStatementCustomizerFactory factory;
        private final Annotation                    annotation;

        FactoryAnnotationPair(SqlStatementCustomizerFactory factory, Annotation annotation)
        {
            this.factory = factory;
            this.annotation = annotation;
        }
    }

    private static class FactoryAnnotationIndexTriple
    {
        private final SqlStatementCustomizerFactory factory;
        private final Annotation                    annotation;
        private final int                           index;

        FactoryAnnotationIndexTriple(SqlStatementCustomizerFactory factory, Annotation annotation, int index)
        {
            this.factory = factory;
            this.annotation = annotation;
            this.index = index;
        }
    }

}
