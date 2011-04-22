package org.skife.jdbi.v2.sqlobject;

import com.fasterxml.classmate.members.ResolvedMethod;
import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.sqlobject.binders.BinderFactory;
import org.skife.jdbi.v2.sqlobject.binders.BindingAnnotation;
import org.skife.jdbi.v2.sqlobject.customizers.CustomizerAnnotation;
import org.skife.jdbi.v2.tweak.StatementCustomizer;
import org.skife.jdbi.v2.sqlobject.customizers.StatementCustomizerFactory;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

abstract class CustomizingStatementHandler implements Handler
{
    private final List<MethodCustomizer>       methodCustomizers    = new ArrayList<MethodCustomizer>();
    private final List<ParameterCustomizer>    paramCustomizers     = new ArrayList<ParameterCustomizer>();
    private final List<Bindifier>              binders              = new ArrayList<Bindifier>();
    private final List<SQLStatementCustomizer> statementCustomizers = new ArrayList<SQLStatementCustomizer>();

    CustomizingStatementHandler(Class sqlObjectType, ResolvedMethod method)
    {

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
                SQLStatementCustomizer c = f.create(annotation, sqlObjectType, method.getRawMember());
                statementCustomizers.add(c);
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

    protected void applySqlStatementCustomizers(SQLStatement q)
    {
        for (SQLStatementCustomizer customizer : statementCustomizers) {
            customizer.apply(q);
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
}
