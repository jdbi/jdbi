package org.skife.jdbi.v2.unstable.eod;

import com.fasterxml.classmate.members.ResolvedMethod;
import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.tweak.StatementCustomizer;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

abstract class BaseHandler implements Handler
{
    private final List<MethodCustomizer> methodCustomizers = new ArrayList<MethodCustomizer>();
    private final List<ParameterCustomizer> paramCustomizers  = new ArrayList<ParameterCustomizer>();

    BaseHandler(ResolvedMethod method)
    {
        Annotation[] method_annotations = method.getRawMember().getAnnotations();
        for (Annotation method_annotation : method_annotations) {
            Class<? extends Annotation> m_anno_class = method_annotation.annotationType();
            if (m_anno_class.isAnnotationPresent(Customizer.class)) {
                Customizer c = m_anno_class.getAnnotation(Customizer.class);
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

                if (anno_class.isAnnotationPresent(Customizer.class)) {
                    // we have a customizer annotation on one of the parameters
                    Customizer ca = annotation.annotationType().getAnnotation(Customizer.class);
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

    protected List<ParameterCustomizer> getParamCustomizers()
    {
        return paramCustomizers;
    }

    protected List<MethodCustomizer> getMethodCustomizers()
    {
        return methodCustomizers;
    }

    protected void applyCustomizers(SQLStatement q, Object[] args) {
        for (MethodCustomizer customizer : getMethodCustomizers()) {
            q.addStatementCustomizer(customizer.build());
        }

        for (ParameterCustomizer customizer : getParamCustomizers()) {
            q.addStatementCustomizer(customizer.build(args[customizer.getIndex()]));
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
