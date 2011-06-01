package org.skife.jdbi.v2.sqlobject;

import com.fasterxml.classmate.members.ResolvedMethod;
import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.sqlobject.binders.BinderFactory;
import org.skife.jdbi.v2.sqlobject.binders.BindingAnnotation;
import org.skife.jdbi.v2.tweak.StatementCustomizer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

abstract class CustomizingStatementHandler implements Handler
{
    private final List<MethodCustomizer>             methodCustomizers              = new ArrayList<MethodCustomizer>();
    private final List<ParameterCustomizer>          paramCustomizers               = new ArrayList<ParameterCustomizer>();
    private final List<Bindifier>                    binders                        = new ArrayList<Bindifier>();
    private final List<FactoryAnnotationPair>        typeBasedCustomizerFactories   = new ArrayList<FactoryAnnotationPair>();
    private final List<FactoryAnnotationPair>        methodBasedCustomizerFactories = new ArrayList<FactoryAnnotationPair>();
    private final List<FactoryAnnotationIndexTriple> paramBasedCustomizerFactories  = new ArrayList<FactoryAnnotationIndexTriple>();
    private final Class sqlObjectType;
    private final Method method;

    CustomizingStatementHandler(Class sqlObjectType, ResolvedMethod method)
    {
        this.sqlObjectType = sqlObjectType;
        this.method = method.getRawMember();

        for (Annotation annotation : sqlObjectType.getAnnotations()) {
            if (annotation.annotationType().isAnnotationPresent(SQLStatementCustomizingAnnotation.class)) {
                SQLStatementCustomizingAnnotation a = annotation.annotationType()
                                                                .getAnnotation(SQLStatementCustomizingAnnotation.class);
                final SQLStatementCustomizerFactory f;
                try {
                    f = a.value().newInstance();
                }
                catch (Exception e) {
                    throw new IllegalStateException("unable to create sql statement customizer factory", e);
                }
                typeBasedCustomizerFactories.add(new FactoryAnnotationPair(f, annotation));
            }
        }


        Annotation[] method_annotations = method.getRawMember().getAnnotations();
        for (Annotation method_annotation : method_annotations) {
            Class<? extends Annotation> m_anno_class = method_annotation.annotationType();
            if (m_anno_class.isAnnotationPresent(CustomizerAnnotation.class)) {
                CustomizerAnnotation c = m_anno_class.getAnnotation(CustomizerAnnotation.class);
                try {
                    StatementCustomizerFactory fact = c.value().newInstance();
                    methodCustomizers.add(new MethodCustomizer(fact, method_annotation));
                }
                catch (Exception e) {
                    throw new IllegalStateException("unable to create a method customizer", e);
                }
            }

            if (m_anno_class.isAnnotationPresent(SQLStatementCustomizingAnnotation.class)) {
                final SQLStatementCustomizingAnnotation scf = m_anno_class.getAnnotation(SQLStatementCustomizingAnnotation.class);
                SQLStatementCustomizerFactory f = null;
                try {
                    f = scf.value().newInstance();
                }
                catch (Exception e) {
                    throw new IllegalStateException("unable to instantiate statement customizer factory", e);
                }
                methodBasedCustomizerFactories.add(new FactoryAnnotationPair(f, method_annotation));
            }

        }

        Annotation[][] param_annotations = method.getRawMember().getParameterAnnotations();
        for (int param_idx = 0; param_idx < param_annotations.length; param_idx++) {
            Annotation[] annotations = param_annotations[param_idx];
            for (Annotation annotation : annotations) {
                Class<? extends Annotation> anno_class = annotation.annotationType();


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


                if (anno_class.isAnnotationPresent(CustomizerAnnotation.class)) {
                    // we have a customizer annotation on one of the parameters
                    CustomizerAnnotation ca = annotation.annotationType().getAnnotation(CustomizerAnnotation.class);
                    try {
                        StatementCustomizerFactory fact = ca.value().newInstance();
                        paramCustomizers.add(new ParameterCustomizer(annotation, fact, param_idx));

                    }
                    catch (Exception e) {
                        throw new IllegalStateException("unable to instantiate cusotmizer", e);
                    }
                }

                if (anno_class.isAnnotationPresent(SQLStatementCustomizingAnnotation.class)) {
                    SQLStatementCustomizingAnnotation sca = annotation.annotationType()
                                                                      .getAnnotation(SQLStatementCustomizingAnnotation.class);
                    final SQLStatementCustomizerFactory f;
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

    protected void applyBinders(SQLStatement q, Object[] args)
    {
        for (Bindifier binder : binders) {
            binder.bind(q, args);
        }
    }

    protected void applyCustomizers(SQLStatement q, Object[] args)
    {
        for (MethodCustomizer customizer : methodCustomizers) {
            q.addStatementCustomizer(customizer.build());
        }

        for (ParameterCustomizer customizer : paramCustomizers) {
            q.addStatementCustomizer(customizer.build(args[customizer.getIndex()]));
        }
    }

    protected void applySqlStatementCustomizers(SQLStatement q, Object[] args) throws SQLException
    {
        for (FactoryAnnotationPair pair : typeBasedCustomizerFactories) {
            pair.factory.createForType(pair.annotation, sqlObjectType).apply(q);
        }

        for (FactoryAnnotationPair pair : methodBasedCustomizerFactories) {
            pair.factory.createForMethod(pair.annotation, sqlObjectType, method).apply(q);
        }

        for (FactoryAnnotationIndexTriple triple : paramBasedCustomizerFactories) {
            triple.factory.createForParameter(triple.annotation, sqlObjectType, method, args[triple.index]).apply(q);
        }
    }


    protected class MethodCustomizer
    {

        private final StatementCustomizerFactory factory;
        private final Annotation                 annotation;

        public MethodCustomizer(StatementCustomizerFactory factory, Annotation annotation)
        {
            this.factory = factory;
            this.annotation = annotation;
        }

        StatementCustomizer build()
        {
            return factory.createForMethod(annotation);
        }

    }

    protected class ParameterCustomizer
    {
        private final Annotation                 annotation;
        private final StatementCustomizerFactory factory;
        private final int                        index;

        ParameterCustomizer(Annotation annotation, StatementCustomizerFactory factory, int idx)
        {
            this.annotation = annotation;
            this.factory = factory;
            this.index = idx;
        }

        public int getIndex()
        {
            return index;
        }

        public StatementCustomizer build(Object arg)
        {
            return factory.createForParameter(annotation, arg);
        }
    }


    private static class FactoryAnnotationPair
    {
        private final SQLStatementCustomizerFactory factory;
        private final Annotation                    annotation;

        FactoryAnnotationPair(SQLStatementCustomizerFactory factory, Annotation annotation)
        {
            this.factory = factory;
            this.annotation = annotation;
        }
    }

    private static class FactoryAnnotationIndexTriple
    {
        private final SQLStatementCustomizerFactory factory;
        private final Annotation                    annotation;
        private final int                           index;

        FactoryAnnotationIndexTriple(SQLStatementCustomizerFactory factory, Annotation annotation, int index)
        {
            this.factory = factory;
            this.annotation = annotation;
            this.index = index;
        }
    }

}
