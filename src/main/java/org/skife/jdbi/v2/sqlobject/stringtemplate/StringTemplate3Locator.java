package org.skife.jdbi.v2.sqlobject.stringtemplate;

import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.antlr.stringtemplate.language.AngleBracketTemplateLexer;
import org.skife.jdbi.v2.ColonPrefixNamedParamStatementRewriter;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.sqlobject.StatementLocatorAnnotation;
import org.skife.jdbi.v2.sqlobject.StatementLocatorFactory;
import org.skife.jdbi.v2.sqlobject.StatementRewriterFactory;
import org.skife.jdbi.v2.tweak.StatementLocator;
import org.skife.jdbi.v2.tweak.StatementRewriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Map;

@StatementLocatorAnnotation(StringTemplate3Locator.LocatorFactory.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface StringTemplate3Locator
{
    static final String DEFAULT_VALUE = " ~ ";

    String value() default DEFAULT_VALUE;

    public static class LocatorFactory implements StatementLocatorFactory
    {
        private final static String sep = System.getProperty("file.separator");

        private static String mungify(String path)
        {
            return path.replaceAll("\\.", sep);
        }

        public StatementLocator create(Annotation anno, Class sqlObjectType, StatementLocator parent)
        {
            final String base;
            final StringTemplate3Locator a = (StringTemplate3Locator) anno;
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
            return new StatementLocator()
            {
                public String locate(String name, StatementContext ctx) throws Exception
                {
                    StringTemplate t = group.lookupTemplate(name);
                    for (Map.Entry<String, Object> entry : ctx.getAttributes().entrySet()) {
                        t.setAttribute(entry.getKey(), entry.getValue());
                    }
                    return t.toString();
                }
            };
        }
    }
}
