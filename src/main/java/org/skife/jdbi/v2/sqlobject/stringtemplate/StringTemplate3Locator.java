package org.skife.jdbi.v2.sqlobject.stringtemplate;

import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.antlr.stringtemplate.language.AngleBracketTemplateLexer;
import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.sqlobject.SQLStatementCustomizer;
import org.skife.jdbi.v2.sqlobject.SQLStatementCustomizerFactory;
import org.skife.jdbi.v2.sqlobject.SQLStatementCustomizingAnnotation;
import org.skife.jdbi.v2.tweak.StatementLocator;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.Map;

@SQLStatementCustomizingAnnotation(StringTemplate3Locator.LocatorFactory.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface StringTemplate3Locator
{
    static final String DEFAULT_VALUE = " ~ ";

    String value() default DEFAULT_VALUE;

    public static class LocatorFactory implements SQLStatementCustomizerFactory
    {
        private final static String sep = System.getProperty("file.separator");

        private static String mungify(String path)
        {
            return path.replaceAll("\\.", sep);
        }

        public SQLStatementCustomizer createForType(Annotation annotation, Class sqlObjectType)
        {
            final String base;
            final StringTemplate3Locator a = (StringTemplate3Locator) annotation;
            if (DEFAULT_VALUE.equals(a.value())) {
                base = mungify("/" + sqlObjectType.getName()) + ".sql.stg";
            }
            else {
                base = a.value();
            }

            InputStream ins = getClass().getResourceAsStream(base);
            InputStreamReader reader = new InputStreamReader(ins);
            final StringTemplateGroup group = new StringTemplateGroup(reader, AngleBracketTemplateLexer.class);
            try {
                reader.close();
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
            final StatementLocator l = new StatementLocator()
            {
                public String locate(String name, StatementContext ctx) throws Exception
                {
                    if (group.isDefined(name)) {
                        StringTemplate t = group.lookupTemplate(name);
                        for (Map.Entry<String, Object> entry : ctx.getAttributes().entrySet()) {
                            t.setAttribute(entry.getKey(), entry.getValue());
                        }
                        return t.toString();
                    }
                    else {
                        // no template matches name, so just return it
                        return name;
                    }

                }
            };

            return new SQLStatementCustomizer()
            {
                public void apply(SQLStatement q)
                {
                    q.setStatementLocator(l);
                }
            };
        }

        public SQLStatementCustomizer createForMethod(Annotation annotation, Class sqlObjectType, Method method)
        {
            throw new UnsupportedOperationException("Not Defined on Method");
        }

        public SQLStatementCustomizer createForParameter(Annotation annotation, Class sqlObjectType, Method method, Object arg)
        {
            throw new UnsupportedOperationException("Not defined on parameter");
        }
    }
}
